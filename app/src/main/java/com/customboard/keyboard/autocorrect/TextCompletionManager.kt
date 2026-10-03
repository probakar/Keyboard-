package com.customboard.keyboard.autocorrect

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/**
 * "Smart compose": offers a multi-word continuation for the sentence being typed.
 * Works offline by walking the bigram chain; the AI module can replace it with a Gemini
 * completion when the user enables cloud AI.
 */
class TextCompletionManager(context: Context) {

    private val model = LanguageModelManager.getInstance(context)
    private val prefs = PreferencesManager.getInstance(context)

    private val phrases = mapOf(
        "thank" to "you so much",
        "thanks" to "for your help",
        "let me" to "know if you need anything",
        "looking" to "forward to hearing from you",
        "please" to "let me know",
        "i hope" to "you are doing well",
        "best" to "regards",
        "sorry" to "for the late reply",
        "can you" to "please help me with",
        "see you" to "soon",
        "happy" to "birthday",
        "good" to "morning",
        "have a" to "great day",
        "i will" to "get back to you",
        "attached" to "is the document you asked for"
    )

    /** Returns the continuation (without the already typed text) or null. */
    fun complete(textBeforeCursor: CharSequence): String? {
        if (!prefs.smartCompose || !model.isLoaded) return null
        val text = textBeforeCursor.toString().trimEnd()
        if (text.isEmpty() || text.length < 3) return null
        if (!text.last().isLetter() && text.last() != ' ') return null

        val lower = text.lowercase()
        phrases.entries
            .filter { lower.endsWith(it.key) }
            .maxByOrNull { it.key.length }
            ?.let { return it.value }

        // Fall back to a two word bigram walk.
        val lastWord = lower.split(Regex("\\s+")).lastOrNull { it.isNotBlank() } ?: return null
        if (lastWord.length < 2) return null
        val first = model.nextWords(lastWord, 1).firstOrNull() ?: return null
        val second = model.nextWords(first, 1).firstOrNull()
        return if (second != null) "$first $second" else first
    }
}
