package com.customboard.keyboard.textprocessing

/** Find and replace used by the text tools panel. */
object FindReplaceManager {

    data class Match(val start: Int, val end: Int)

    fun find(text: CharSequence, query: String, ignoreCase: Boolean = true): List<Match> {
        if (query.isEmpty()) return emptyList()
        val matches = mutableListOf<Match>()
        var index = text.indexOf(query, 0, ignoreCase)
        while (index >= 0) {
            matches += Match(index, index + query.length)
            index = text.indexOf(query, index + query.length, ignoreCase)
        }
        return matches
    }

    fun replaceAll(
        text: String,
        query: String,
        replacement: String,
        ignoreCase: Boolean = true
    ): String = if (query.isEmpty()) text else text.replace(query, replacement, ignoreCase)

    fun replaceFirst(
        text: String,
        query: String,
        replacement: String,
        ignoreCase: Boolean = true
    ): String {
        val index = text.indexOf(query, 0, ignoreCase)
        if (index < 0 || query.isEmpty()) return text
        return text.substring(0, index) + replacement + text.substring(index + query.length)
    }

    fun count(text: CharSequence, query: String, ignoreCase: Boolean = true): Int =
        find(text, query, ignoreCase).size
}
