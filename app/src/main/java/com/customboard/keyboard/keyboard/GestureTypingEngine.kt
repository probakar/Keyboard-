package com.customboard.keyboard.keyboard

import android.content.Context
import android.graphics.PointF
import com.customboard.keyboard.autocorrect.LanguageModelManager
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

/**
 * Glide / swipe typing.
 *
 * The finger path is resampled to a fixed number of points, then every dictionary word whose
 * first and last letters match the ends of the gesture is scored by how closely the key
 * centres of its letters follow the path (a monotonic nearest-point walk, i.e. a cheap DTW).
 * Scoring runs off the UI thread, typically in a couple of milliseconds.
 */
class GestureTypingEngine(context: Context) {

    private val model = LanguageModelManager.getInstance(context)

    data class Result(val words: List<String>) {
        val best: String? get() = words.firstOrNull()
    }

    /**
     * @param points          raw touch path
     * @param letterPositions centre of every letter key currently on screen
     */
    fun recognize(
        points: List<PointF>,
        letterPositions: Map<Char, PointF>,
        previousWord: String? = null,
        limit: Int = 5
    ): Result {
        if (points.size < 3 || letterPositions.isEmpty() || !model.isLoaded) return Result(emptyList())

        val path = resample(points, SAMPLES)
        val first = nearestLetter(path.first(), letterPositions) ?: return Result(emptyList())
        val last = nearestLetter(path.last(), letterPositions) ?: return Result(emptyList())

        val keyRadius = estimateKeyRadius(letterPositions)
        val pathLength = pathLength(path)
        val maxWordLength = (pathLength / (keyRadius * 0.55f)).toInt().coerceIn(3, 18)

        val firstCandidates = (letterPositions.keys.filter {
            it == first || distance(letterPositions[it]!!, letterPositions[first]!!) < keyRadius * 1.1f
        }).toSet()
        val lastCandidates = (letterPositions.keys.filter {
            it == last || distance(letterPositions[it]!!, letterPositions[last]!!) < keyRadius * 1.1f
        }).toSet()

        val scored = ArrayList<Pair<String, Double>>(64)
        for (word in model.allWords()) {
            if (word.length < 2 || word.length > maxWordLength) continue
            if (!firstCandidates.contains(word.first())) continue
            if (!lastCandidates.contains(word.last())) continue
            if (word.any { letterPositions[it] == null }) continue
            val cost = score(word, path, letterPositions, keyRadius) ?: continue
            val frequencyBonus = model.frequency(word) / 255.0 * 0.45
            val bigramBonus = previousWord?.let {
                min(model.bigramWeight(it, word), 100) / 100.0 * 0.25
            } ?: 0.0
            scored += word to (cost - frequencyBonus - bigramBonus)
        }

        if (scored.isEmpty()) return Result(emptyList())
        return Result(scored.sortedBy { it.second }.take(limit).map { it.first })
    }

    /** Average normalised distance between the word's key centres and the gesture path. */
    private fun score(
        word: String,
        path: List<PointF>,
        letters: Map<Char, PointF>,
        keyRadius: Float
    ): Double? {
        var pathIndex = 0
        var total = 0.0
        for (char in word) {
            val target = letters[char] ?: return null
            var bestDistance = Float.MAX_VALUE
            var bestIndex = pathIndex
            // walk forward only: the gesture visits the letters in order
            var index = pathIndex
            while (index < path.size) {
                val d = distance(path[index], target)
                if (d < bestDistance) {
                    bestDistance = d
                    bestIndex = index
                }
                index++
            }
            if (bestDistance > keyRadius * 2.6f) return null
            total += (bestDistance / keyRadius).toDouble()
            pathIndex = bestIndex
        }
        // Penalise words that are much shorter than the drawn path.
        val lengthPenalty = abs(path.size / SAMPLES.toDouble() - 1.0)
        return total / word.length + lengthPenalty * 0.1
    }

    private fun resample(points: List<PointF>, count: Int): List<PointF> {
        if (points.size <= count) return points
        val step = points.size / count.toFloat()
        return (0 until count).map { points[(it * step).toInt().coerceIn(0, points.size - 1)] }
    }

    private fun pathLength(points: List<PointF>): Float {
        var length = 0f
        for (i in 1 until points.size) length += distance(points[i - 1], points[i])
        return length
    }

    private fun nearestLetter(point: PointF, letters: Map<Char, PointF>): Char? {
        var best: Char? = null
        var bestDistance = Float.MAX_VALUE
        for ((char, center) in letters) {
            val d = distance(point, center)
            if (d < bestDistance) {
                bestDistance = d
                best = char
            }
        }
        return best
    }

    private fun estimateKeyRadius(letters: Map<Char, PointF>): Float {
        if (letters.size < 2) return 40f
        val values = letters.values.toList()
        var minDistance = Float.MAX_VALUE
        for (i in values.indices) {
            for (j in i + 1 until values.size) {
                val d = distance(values[i], values[j])
                if (d > 1f && d < minDistance) minDistance = d
            }
        }
        return if (minDistance == Float.MAX_VALUE) 40f else minDistance.coerceIn(24f, 160f)
    }

    private fun distance(a: PointF, b: PointF): Float = hypot(a.x - b.x, a.y - b.y)

    companion object {
        private const val SAMPLES = 60
    }
}
