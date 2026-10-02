package com.customboard.keyboard.autocorrect

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/** Next-word prediction driven by the bigram table with a frequency fallback. */
class WordPredictionEngine(context: Context) {

    private val model = LanguageModelManager.getInstance(context)
    private val prefs = PreferencesManager.getInstance(context)

    private val sentenceStarters = listOf("i", "the", "thanks", "hello", "we", "it", "can", "please")

    fun predict(previousWord: String?, limit: Int = 3): List<Suggestion> {
        if (!model.isLoaded) return emptyList()
        val results = LinkedHashMap<String, Suggestion>()

        if (!previousWord.isNullOrBlank()) {
            model.nextWords(previousWord, limit + 2).forEachIndexed { index, word ->
                results[word] = Suggestion(
                    word = word,
                    score = 900.0 - index * 10,
                    source = Suggestion.Source.PREDICTION
                )
            }
        }

        if (results.size < limit) {
            val fallback = if (previousWord.isNullOrBlank()) sentenceStarters
            else listOf("the", "to", "and", "is", "you")
            fallback.forEach { word ->
                if (results.size >= limit + 2) return@forEach
                if (!results.containsKey(word)) {
                    results[word] = Suggestion(word, 200.0, Suggestion.Source.PREDICTION)
                }
            }
        }

        return results.values
            .filterNot { prefs.blockOffensive && OffensiveWordFilter.isOffensive(it.word) }
            .take(limit)
    }
}
