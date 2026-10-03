package com.customboard.keyboard.autocorrect

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants

/**
 * Produces the three (or more) candidates shown above the keys and decides whether the word
 * being typed should be replaced automatically when the user hits space.
 */
class AutoCorrectionEngine(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val model = LanguageModelManager.getInstance(context)
    private val spellChecker = SpellChecker(model)
    private val personal = PersonalDictionary(context)
    private val prediction = WordPredictionEngine(context)

    fun ensureLoaded(onReady: (() -> Unit)? = null) = model.loadAsync(onReady)

    val isReady: Boolean get() = model.isLoaded

    /**
     * @param composing the word currently being typed (may be empty)
     * @param previousWord the word before it, used for bigram prediction
     */
    fun suggestions(composing: String, previousWord: String?): List<Suggestion> {
        if (!model.isLoaded) return emptyList()

        if (composing.isEmpty()) {
            return if (prefs.prediction) prediction.predict(previousWord) else emptyList()
        }

        val input = composing.trim()
        if (input.isEmpty()) return emptyList()
        val lower = input.lowercase()
        val results = LinkedHashMap<String, Suggestion>()

        // 1. the literal input always stays available so the user can keep what they typed
        val exactKnown = model.contains(lower)
        results[input] = Suggestion(
            word = input,
            score = if (exactKnown) 1_000.0 else 500.0,
            source = Suggestion.Source.EXACT
        )

        // 2. completions of the current prefix
        if (input.length >= 2) {
            model.wordsWithPrefix(lower, 8).forEach { word ->
                if (word == lower) return@forEach
                val bigramBoost = previousWord?.let { model.bigramWeight(it, word) } ?: 0
                val score = 400.0 + model.frequency(word) + bigramBoost * 2.0 -
                    (word.length - input.length) * 6
                results.putIfAbsentCompat(word, Suggestion(word, score, Suggestion.Source.COMPLETION))
            }
        }

        // 3. spelling corrections
        if (prefs.autoCorrect && !exactKnown && input.length >= 2) {
            spellChecker.suggestions(lower, 6).forEachIndexed { index, word ->
                val score = 600.0 + model.frequency(word) - index * 40
                results.putIfAbsentCompat(word, Suggestion(word, score, Suggestion.Source.CORRECTION))
            }
        }

        val ordered = results.values
            .asSequence()
            .filterNot { personal.isBlocked(it.word) }
            .filterNot { prefs.blockOffensive && OffensiveWordFilter.isOffensive(it.word) }
            .sortedByDescending { it.score }
            .take(Constants.MAX_SUGGESTIONS)
            .toMutableList()

        // 4. decide the auto-correction (Gboard shows it as the bold middle candidate)
        if (prefs.autoCorrect && !exactKnown) {
            val best = ordered.firstOrNull {
                it.source == Suggestion.Source.CORRECTION || it.source == Suggestion.Source.COMPLETION
            }
            if (best != null && shouldAutoCorrect(input, best)) {
                val index = ordered.indexOf(best)
                ordered[index] = best.copy(isAutoCorrection = true)
            }
        }

        return applyCase(input, ordered)
    }

    /** The replacement to commit when the user types a separator, or null to keep the word. */
    fun autoCorrectionFor(composing: String, previousWord: String?): String? {
        if (!prefs.autoCorrect || composing.length < 2) return null
        if (model.contains(composing.lowercase())) return null
        if (composing.any { !it.isLetter() && it != '\'' }) return null
        val suggestion = suggestions(composing, previousWord)
            .firstOrNull { it.isAutoCorrection } ?: return null
        return suggestion.word.takeIf { !it.equals(composing, ignoreCase = true) }
    }

    private fun shouldAutoCorrect(input: String, candidate: Suggestion): Boolean {
        if (input.length < 3) return false
        if (candidate.word.equals(input, ignoreCase = true)) return false
        if (personal.isBlocked(candidate.word)) return false
        val distance = spellChecker.weightedDistance(input.lowercase(), candidate.word, 3)
        return when {
            distance <= 1.0 -> true
            distance <= 2.0 && input.length >= 5 -> true
            else -> false
        }
    }

    /** Mirrors the capitalisation of the typed word onto the suggestions. */
    private fun applyCase(input: String, suggestions: List<Suggestion>): List<Suggestion> {
        val allCaps = input.length > 1 && input.all { it.isUpperCase() }
        val capitalised = input.firstOrNull()?.isUpperCase() == true
        if (!allCaps && !capitalised) return suggestions
        return suggestions.map { suggestion ->
            val word = when {
                allCaps -> suggestion.word.uppercase()
                capitalised -> suggestion.word.replaceFirstChar { it.uppercase() }
                else -> suggestion.word
            }
            suggestion.copy(word = word)
        }
    }

    fun learn(word: String, previousWord: String?) = model.learn(word, previousWord)

    fun addToDictionary(word: String) = personal.add(word)

    fun blockWord(word: String) = personal.block(word)

    fun persist() = model.persist()

    private fun <K, V> LinkedHashMap<K, V>.putIfAbsentCompat(key: K, value: V) {
        if (!containsKey(key)) put(key, value)
    }
}
