package com.customboard.keyboard.autocorrect

/**
 * Minimal profanity filter used only to decide whether a word may be *suggested*.
 * Typing anything is always allowed - the keyboard never censors the user.
 */
object OffensiveWordFilter {

    private val BLOCKED = setOf(
        "fuck", "fucking", "fucked", "shit", "shitty", "bitch", "bastard", "asshole",
        "dick", "cunt", "whore", "slut", "nigger", "faggot", "rape", "porn", "sex",
        "cock", "pussy", "wanker", "motherfucker", "retard", "nazi"
    )

    fun isOffensive(word: String): Boolean = BLOCKED.contains(word.trim().lowercase())

    fun filter(words: List<String>, enabled: Boolean): List<String> =
        if (!enabled) words else words.filterNot { isOffensive(it) }
}
