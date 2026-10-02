package com.customboard.keyboard.emoji

import android.content.Context
import android.util.Log
import com.customboard.keyboard.R
import java.io.BufferedReader
import java.io.InputStreamReader

/** An emoji together with the words it can be searched by. */
data class EmojiItem(
    val emoji: String,
    val category: String,
    val keywords: String
) {
    val supportsSkinTone: Boolean
        get() = SKIN_TONE_HINTS.any { keywords.contains(it) }

    companion object {
        private val SKIN_TONE_HINTS = listOf(
            "hand", "finger", "person", "man", "woman", "boy", "girl", "baby", "nose", "ear",
            "foot", "leg", "arm", "thumbs", "clapping", "waving", "writing", "nail", "muscle",
            "pray", "vulcan", "victory", "ok hand", "call me", "point", "raised"
        )
    }
}

/**
 * Loads the bundled emoji table (categories, names and search keywords) that is generated from
 * the Unicode data at build time, and exposes it to the picker.
 */
class EmojiManager private constructor(private val context: Context) {

    companion object {
        const val CATEGORY_RECENT = "recent"
        const val CATEGORY_SMILEYS = "smileys"
        const val CATEGORY_PEOPLE = "people"
        const val CATEGORY_ANIMALS = "animals"
        const val CATEGORY_FOOD = "food"
        const val CATEGORY_TRAVEL = "travel"
        const val CATEGORY_ACTIVITIES = "activities"
        const val CATEGORY_OBJECTS = "objects"
        const val CATEGORY_SYMBOLS = "symbols"
        const val CATEGORY_FLAGS = "flags"
        const val CATEGORY_KAOMOJI = "kaomoji"

        val CATEGORY_ORDER = listOf(
            CATEGORY_RECENT, CATEGORY_SMILEYS, CATEGORY_PEOPLE, CATEGORY_ANIMALS, CATEGORY_FOOD,
            CATEGORY_TRAVEL, CATEGORY_ACTIVITIES, CATEGORY_OBJECTS, CATEGORY_SYMBOLS,
            CATEGORY_FLAGS, CATEGORY_KAOMOJI
        )

        private const val TAG = "EmojiManager"

        @Volatile
        private var instance: EmojiManager? = null

        fun getInstance(context: Context): EmojiManager =
            instance ?: synchronized(this) {
                instance ?: EmojiManager(context.applicationContext).also { instance = it }
            }
    }

    private val byCategory = LinkedHashMap<String, MutableList<EmojiItem>>()
    private val all = ArrayList<EmojiItem>(1200)

    @Volatile
    var isLoaded = false
        private set

    @Synchronized
    fun load() {
        if (isLoaded) return
        runCatching {
            context.resources.openRawResource(R.raw.emoji_data).use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    if (line.isEmpty() || line.startsWith("#")) return@forEachLine
                    val parts = line.split('\t')
                    if (parts.size < 3) return@forEachLine
                    val item = EmojiItem(parts[0], parts[1], parts[2])
                    all += item
                    byCategory.getOrPut(parts[1]) { ArrayList(128) } += item
                }
            }
        }.onFailure { Log.w(TAG, "emoji load failed", it) }
        isLoaded = true
    }

    fun categories(): List<String> = CATEGORY_ORDER

    fun emojisFor(category: String): List<EmojiItem> {
        if (!isLoaded) load()
        return byCategory[category] ?: emptyList()
    }

    fun allEmojis(): List<EmojiItem> {
        if (!isLoaded) load()
        return all
    }

    fun find(emoji: String): EmojiItem? = all.firstOrNull { it.emoji == emoji }

    fun categoryTitleRes(category: String): Int = when (category) {
        CATEGORY_RECENT -> R.string.emoji_recent
        CATEGORY_SMILEYS -> R.string.emoji_smileys
        CATEGORY_PEOPLE -> R.string.emoji_people
        CATEGORY_ANIMALS -> R.string.emoji_animals
        CATEGORY_FOOD -> R.string.emoji_food
        CATEGORY_TRAVEL -> R.string.emoji_travel
        CATEGORY_ACTIVITIES -> R.string.emoji_activities
        CATEGORY_OBJECTS -> R.string.emoji_objects
        CATEGORY_SYMBOLS -> R.string.emoji_symbols
        CATEGORY_FLAGS -> R.string.emoji_flags
        else -> R.string.emoji_kaomoji
    }

    /** Icon drawn on the category tab. */
    fun categoryIcon(category: String): String = when (category) {
        CATEGORY_RECENT -> "🕘"
        CATEGORY_SMILEYS -> "😀"
        CATEGORY_PEOPLE -> "👋"
        CATEGORY_ANIMALS -> "🐱"
        CATEGORY_FOOD -> "🍕"
        CATEGORY_TRAVEL -> "✈"
        CATEGORY_ACTIVITIES -> "⚽"
        CATEGORY_OBJECTS -> "💡"
        CATEGORY_SYMBOLS -> "❤"
        CATEGORY_FLAGS -> "🏳"
        else -> "^_^"
    }
}
