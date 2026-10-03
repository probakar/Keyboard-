package com.customboard.keyboard.autocorrect

import android.content.Context
import android.util.Log
import com.customboard.keyboard.R
import com.customboard.keyboard.settings.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * The n-gram language model: a frequency dictionary (unigrams) plus a bigram table that is
 * continuously enriched from what the user types. Everything stays on the device.
 */
class LanguageModelManager private constructor(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** word -> frequency 0..255 */
    private val unigrams = HashMap<String, Int>(6000)

    /** "previous word" -> (next word -> weight) */
    private val bigrams = HashMap<String, HashMap<String, Int>>(1500)

    /** Lexicographically sorted words, used for fast prefix lookups. */
    @Volatile
    private var sortedWords: Array<String> = emptyArray()

    @Volatile
    var isLoaded = false
        private set

    fun loadAsync(onReady: (() -> Unit)? = null) {
        if (isLoaded) {
            onReady?.invoke()
            return
        }
        scope.launch {
            load()
            onReady?.invoke()
        }
    }

    @Synchronized
    fun load() {
        if (isLoaded) return
        runCatching { readDictionary() }.onFailure { Log.w(TAG, "dictionary load failed", it) }
        runCatching { readBigrams() }.onFailure { Log.w(TAG, "bigram load failed", it) }
        runCatching { readUserData() }.onFailure { Log.w(TAG, "user data load failed", it) }
        sortedWords = unigrams.keys.toTypedArray().also { it.sort() }
        isLoaded = true
    }

    private fun readDictionary() {
        context.resources.openRawResource(R.raw.dictionary_en).use { stream ->
            BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine
                val tab = line.indexOf('\t')
                if (tab <= 0) return@forEachLine
                val word = line.substring(0, tab)
                val freq = line.substring(tab + 1).trim().toIntOrNull() ?: return@forEachLine
                unigrams[word] = freq
            }
        }
    }

    private fun readBigrams() {
        context.resources.openRawResource(R.raw.bigrams_en).use { stream ->
            BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                if (line.isEmpty() || line.startsWith("#")) return@forEachLine
                val parts = line.split('\t')
                if (parts.size < 3) return@forEachLine
                val weight = parts[2].trim().toIntOrNull() ?: 50
                addBigram(parts[0], parts[1], weight)
            }
        }
    }

    private fun readUserData() {
        prefs.unigramData.split('\u0001').forEach { entry ->
            val parts = entry.split('\u0002')
            if (parts.size == 2) {
                val word = parts[0]
                val count = parts[1].toIntOrNull() ?: 0
                if (word.isNotEmpty()) {
                    unigrams[word] = (unigrams[word] ?: 0).coerceAtLeast(count.coerceAtMost(255))
                }
            }
        }
        prefs.bigramData.split('\u0001').forEach { entry ->
            val parts = entry.split('\u0002')
            if (parts.size == 3) {
                addBigram(parts[0], parts[1], parts[2].toIntOrNull() ?: 10)
            }
        }
        prefs.personalDictionary.forEach { word ->
            unigrams[word.lowercase()] = 250
        }
    }

    private fun addBigram(previous: String, next: String, weight: Int) {
        val key = previous.lowercase().trim()
        val value = next.lowercase().trim()
        if (key.isEmpty() || value.isEmpty()) return
        val map = bigrams.getOrPut(key) { HashMap(4) }
        map[value] = (map[value] ?: 0) + weight
    }

    // ------------------------------------------------------------------
    //  Queries
    // ------------------------------------------------------------------

    fun contains(word: String): Boolean = unigrams.containsKey(word.lowercase())

    fun frequency(word: String): Int = unigrams[word.lowercase()] ?: 0

    fun allWords(): Array<String> = sortedWords

    fun wordCount(): Int = unigrams.size

    /** Words starting with [prefix], best first. */
    fun wordsWithPrefix(prefix: String, limit: Int = 12): List<String> {
        if (prefix.isEmpty()) return emptyList()
        val words = sortedWords
        if (words.isEmpty()) return emptyList()
        val lower = prefix.lowercase()
        var low = 0
        var high = words.size - 1
        var start = words.size
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (words[mid] >= lower) {
                start = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        val result = ArrayList<String>(limit)
        var index = start
        while (index < words.size && words[index].startsWith(lower)) {
            result += words[index]
            index++
            if (result.size > 200) break
        }
        return result.sortedByDescending { frequency(it) }.take(limit)
    }

    /** Most likely words to follow [previousWord]. */
    fun nextWords(previousWord: String, limit: Int = 3): List<String> {
        val key = previousWord.lowercase().trim()
        if (key.isEmpty()) return emptyList()
        val direct = bigrams[key] ?: return emptyList()
        return direct.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    fun bigramWeight(previousWord: String, word: String): Int =
        bigrams[previousWord.lowercase()]?.get(word.lowercase()) ?: 0

    // ------------------------------------------------------------------
    //  Learning
    // ------------------------------------------------------------------

    fun learn(word: String, previousWord: String?) {
        if (!prefs.learnWords || prefs.incognito) return
        val clean = word.lowercase().trim()
        if (clean.length < 2 || !clean.all { it.isLetter() || it == '\'' }) return
        unigrams[clean] = (unigrams[clean] ?: 0).coerceAtLeast(40).let { (it + 4).coerceAtMost(255) }
        if (!previousWord.isNullOrBlank()) addBigram(previousWord, clean, 6)
        pendingWrites++
        if (pendingWrites >= 12) persist()
    }

    private var pendingWrites = 0

    /** Persists only the learned part of the model (never the bundled dictionary). */
    @Synchronized
    fun persist() {
        if (prefs.incognito) return
        pendingWrites = 0
        scope.launch {
            runCatching {
                val learned = unigrams.entries
                    .filter { it.value in 41..254 }
                    .sortedByDescending { it.value }
                    .take(2000)
                    .joinToString("\u0001") { "${it.key}\u0002${it.value}" }
                prefs.unigramData = learned

                val pairs = StringBuilder()
                var count = 0
                outer@ for ((previous, map) in bigrams) {
                    for ((next, weight) in map) {
                        if (weight < 6) continue
                        if (count++ > 3000) break@outer
                        if (pairs.isNotEmpty()) pairs.append('\u0001')
                        pairs.append(previous).append('\u0002').append(next)
                            .append('\u0002').append(weight)
                    }
                }
                prefs.bigramData = pairs.toString()
            }
        }
    }

    fun addWord(word: String) {
        val clean = word.lowercase().trim()
        if (clean.isEmpty()) return
        unigrams[clean] = 250
        sortedWords = unigrams.keys.toTypedArray().also { it.sort() }
        prefs.personalDictionary = prefs.personalDictionary + clean
    }

    fun removeWord(word: String) {
        val clean = word.lowercase().trim()
        unigrams.remove(clean)
        sortedWords = unigrams.keys.toTypedArray().also { it.sort() }
        prefs.personalDictionary = prefs.personalDictionary - clean
    }

    fun clearLearnedData() {
        prefs.unigramData = ""
        prefs.bigramData = ""
        bigrams.clear()
        unigrams.clear()
        isLoaded = false
        load()
    }

    companion object {
        private const val TAG = "LanguageModel"

        @Volatile
        private var instance: LanguageModelManager? = null

        fun getInstance(context: Context): LanguageModelManager =
            instance ?: synchronized(this) {
                instance ?: LanguageModelManager(context.applicationContext).also { instance = it }
            }
    }
}
