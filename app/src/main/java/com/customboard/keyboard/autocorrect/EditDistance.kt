package com.customboard.keyboard.autocorrect

import kotlin.math.abs
import kotlin.math.min

/**
 * Keyboard-aware Damerau-Levenshtein distance.
 *
 * Substituting a character with one of its QWERTY neighbours is cheaper than a random
 * substitution, and transpositions ("teh" -> "the") are cheaper than two edits, which is what
 * makes the correction ranking feel natural. Pure Kotlin, so it is fully unit-testable.
 */
object EditDistance {

    /** @return the weighted distance, capped at [maxDistance] + 1. */
    fun weighted(source: String, target: String, maxDistance: Int): Double {
        val n = source.length
        val m = target.length
        if (abs(n - m) > maxDistance) return (maxDistance + 1).toDouble()
        if (n == 0) return m.toDouble()
        if (m == 0) return n.toDouble()

        var previous = DoubleArray(m + 1) { it.toDouble() }
        var beforePrevious = DoubleArray(m + 1) { Double.MAX_VALUE / 4 }
        val current = DoubleArray(m + 1)

        for (i in 1..n) {
            current[0] = i.toDouble()
            var rowMin = current[0]
            for (j in 1..m) {
                val sourceChar = source[i - 1]
                val targetChar = target[j - 1]
                val substitutionCost = when {
                    sourceChar == targetChar -> 0.0
                    areNeighbours(sourceChar, targetChar) -> 0.6
                    else -> 1.0
                }
                var value = min(
                    min(current[j - 1] + 1.0, previous[j] + 1.0),
                    previous[j - 1] + substitutionCost
                )
                // transposition
                if (i > 1 && j > 1 && sourceChar == target[j - 2] && source[i - 2] == targetChar) {
                    value = min(value, beforePrevious[j - 2] + 0.8)
                }
                current[j] = value
                if (value < rowMin) rowMin = value
            }
            if (rowMin > maxDistance) return (maxDistance + 1).toDouble()
            beforePrevious = previous
            previous = current.copyOf()
        }
        return previous[m]
    }

    private val NEIGHBOURS: Map<Char, String> = mapOf(
        'q' to "wa", 'w' to "qes", 'e' to "wrd", 'r' to "etf", 't' to "ryg",
        'y' to "tuh", 'u' to "yij", 'i' to "uok", 'o' to "ipl", 'p' to "o",
        'a' to "qsz", 's' to "awdx", 'd' to "sefc", 'f' to "drgv", 'g' to "fthb",
        'h' to "gyjn", 'j' to "hukm", 'k' to "jil", 'l' to "kop",
        'z' to "asx", 'x' to "zsdc", 'c' to "xdfv", 'v' to "cfgb", 'b' to "vghn",
        'n' to "bhjm", 'm' to "njk"
    )

    /** True when [a] and [b] sit next to each other on a QWERTY keyboard. */
    fun areNeighbours(a: Char, b: Char): Boolean {
        if (a == b) return true
        return NEIGHBOURS[a.lowercaseChar()]?.contains(b.lowercaseChar()) == true
    }
}
