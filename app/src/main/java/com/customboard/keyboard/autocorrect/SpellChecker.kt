package com.customboard.keyboard.autocorrect

import kotlin.math.abs

/**
 * Damerau-Levenshtein spell checking with QWERTY proximity weighting: a typo on a neighbouring
 * key ("teh" -> "the", "amd" -> "and") costs less than an arbitrary substitution.
 */
class SpellChecker(private val model: LanguageModelManager) {

    fun isMisspelled(word: String): Boolean {
        val clean = word.trim().lowercase()
        if (clean.length < 2) return false
        if (!clean.all { it.isLetter() || it == '\'' }) return false
        return !model.contains(clean)
    }

    /** Best corrections for [word], closest first. */
    fun suggestions(word: String, limit: Int = 5): List<String> {
        val input = word.lowercase()
        if (input.isEmpty()) return emptyList()
        val maxDistance = when {
            input.length <= 3 -> 1
            input.length <= 6 -> 2
            else -> 3
        }
        val scored = ArrayList<Pair<String, Double>>(32)
        for (candidate in model.allWords()) {
            if (abs(candidate.length - input.length) > maxDistance) continue
            if (candidate.isNotEmpty() && input.isNotEmpty() &&
                candidate[0] != input[0] && !EditDistance.areNeighbours(candidate[0], input[0])
            ) {
                // The first letter is rarely wrong; skipping keeps the scan fast.
                continue
            }
            val distance = weightedDistance(input, candidate, maxDistance)
            if (distance > maxDistance) continue
            val frequency = model.frequency(candidate) / 255.0
            scored += candidate to (distance - frequency * 0.6)
        }
        return scored.sortedBy { it.second }.take(limit).map { it.first }
    }

    /** Edit distance where substituting a neighbouring key costs 0.6 instead of 1. */
    fun weightedDistance(source: String, target: String, maxDistance: Int): Double =
        EditDistance.weighted(source, target, maxDistance)
}
