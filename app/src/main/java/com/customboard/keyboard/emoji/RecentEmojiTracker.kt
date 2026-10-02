package com.customboard.keyboard.emoji

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants

/** Remembers recently and frequently used emoji (skipped entirely in incognito mode). */
class RecentEmojiTracker(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    fun recent(): List<String> = prefs.recentEmoji
        .split(SEPARATOR)
        .filter { it.isNotEmpty() }

    fun frequent(limit: Int = 24): List<String> = frequencyMap()
        .entries.sortedByDescending { it.value }
        .take(limit)
        .map { it.key }

    fun track(emoji: String) {
        if (prefs.incognito || emoji.isEmpty()) return
        val current = recent().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        while (current.size > Constants.MAX_RECENT_EMOJI) current.removeAt(current.size - 1)
        prefs.recentEmoji = current.joinToString(SEPARATOR)

        val frequencies = frequencyMap().toMutableMap()
        frequencies[emoji] = (frequencies[emoji] ?: 0) + 1
        prefs.emojiFrequency = frequencies.entries
            .sortedByDescending { it.value }
            .take(120)
            .joinToString(SEPARATOR) { "${it.key}$PAIR${it.value}" }
    }

    fun clear() {
        prefs.recentEmoji = ""
        prefs.emojiFrequency = ""
    }

    /** Recent first, then the most frequent ones that are not already listed. */
    fun recentWithFallback(): List<String> {
        val recent = recent()
        if (recent.size >= 16) return recent
        val fallback = DEFAULT_EMOJI.filterNot { recent.contains(it) }
        return recent + fallback.take(32 - recent.size)
    }

    private fun frequencyMap(): Map<String, Int> =
        prefs.emojiFrequency.split(SEPARATOR)
            .mapNotNull { entry ->
                val parts = entry.split(PAIR)
                if (parts.size == 2 && parts[0].isNotEmpty()) {
                    parts[0] to (parts[1].toIntOrNull() ?: 0)
                } else {
                    null
                }
            }.toMap()

    companion object {
        private const val SEPARATOR = "\u0001"
        private const val PAIR = "\u0002"

        val DEFAULT_EMOJI = listOf(
            "😀", "😂", "🤣", "😊", "😍", "🥰", "😎", "🤔", "😢", "😭", "😡", "👍", "👎",
            "🙏", "👏", "🔥", "✨", "🎉", "❤️", "💔", "💯", "✅", "❌", "⭐", "🚀", "☕",
            "🍕", "🎂", "🌹", "🐶", "🐱", "💪"
        )
    }
}
