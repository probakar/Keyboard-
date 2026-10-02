package com.customboard.keyboard.textprocessing

/** Word, character and reading-time counters shown in the text tools panel. */
object TextStatistics {

    data class Stats(
        val characters: Int,
        val charactersNoSpaces: Int,
        val words: Int,
        val sentences: Int,
        val lines: Int,
        val readingSeconds: Int
    )

    fun of(text: CharSequence): Stats {
        val value = text.toString()
        val words = value.split(Regex("\\s+")).count { it.isNotBlank() }
        val sentences = value.split(Regex("[.!?]+")).count { it.isNotBlank() }
        return Stats(
            characters = value.length,
            charactersNoSpaces = value.count { !it.isWhitespace() },
            words = words,
            sentences = sentences,
            lines = if (value.isEmpty()) 0 else value.lines().size,
            readingSeconds = ((words / 200.0) * 60).toInt().coerceAtLeast(if (words > 0) 1 else 0)
        )
    }
}
