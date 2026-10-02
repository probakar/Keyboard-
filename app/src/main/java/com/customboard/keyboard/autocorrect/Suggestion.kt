package com.customboard.keyboard.autocorrect

/** One entry of the suggestion strip. */
data class Suggestion(
    val word: String,
    val score: Double,
    val source: Source,
    val isAutoCorrection: Boolean = false
) {
    enum class Source { EXACT, COMPLETION, CORRECTION, PREDICTION, PERSONAL, CONTACT, CLIPBOARD, AI, GESTURE }
}
