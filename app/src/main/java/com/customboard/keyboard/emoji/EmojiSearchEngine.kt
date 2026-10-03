package com.customboard.keyboard.emoji

import android.content.Context

/** Keyword search over the emoji table ("heart", "fire", "thumbs up"...). */
class EmojiSearchEngine(context: Context) {

    private val manager = EmojiManager.getInstance(context)

    fun search(query: String, limit: Int = 60): List<EmojiItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val terms = q.split(" ").filter { it.isNotBlank() }
        val scored = ArrayList<Pair<EmojiItem, Int>>(64)
        for (item in manager.allEmojis()) {
            var score = 0
            for (term in terms) {
                when {
                    item.keywords == term -> score += 100
                    item.keywords.startsWith(term) -> score += 60
                    item.keywords.contains(" $term") -> score += 40
                    item.keywords.contains(term) -> score += 20
                }
            }
            if (score > 0) scored += item to score
        }
        return scored.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    /** Emoji that match the word the user just typed (shown inline in the suggestion strip). */
    fun suggestForWord(word: String): List<String> {
        if (word.length < 3) return emptyList()
        return search(word, 3).map { it.emoji }
    }
}
