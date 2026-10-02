package com.customboard.keyboard.autocorrect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the correction maths: the keyboard-aware edit distance and the ordering
 * rules the suggestion strip relies on. The dictionary itself needs a Context, so it is covered
 * by the instrumented build instead.
 */
class AutoCorrectionTest {

    @Test
    fun `identical words have no distance`() {
        assertEquals(0.0, EditDistance.weighted("hello", "hello", 3), 0.0001)
    }

    @Test
    fun `a single edit costs less than two`() {
        val one = EditDistance.weighted("helo", "hello", 3)
        val two = EditDistance.weighted("hel", "hello", 3)
        assertTrue("one edit should be cheaper than two", one < two)
    }

    @Test
    fun `neighbouring keys are cheaper than distant ones`() {
        val neighbour = EditDistance.weighted("hwllo", "hello", 3)
        val distant = EditDistance.weighted("hpllo", "hello", 3)
        assertTrue("adjacent key typo should score better", neighbour < distant)
    }

    @Test
    fun `transposition is cheaper than two substitutions`() {
        assertTrue(EditDistance.weighted("teh", "the", 3) < 1.0)
    }

    @Test
    fun `distance is capped by maxDistance`() {
        val far = EditDistance.weighted("abcdefgh", "zyxwvuts", 2)
        assertTrue(far > 2.0)
    }

    @Test
    fun `qwerty neighbours are symmetric for the tested pairs`() {
        assertTrue(EditDistance.areNeighbours('q', 'w'))
        assertTrue(EditDistance.areNeighbours('w', 'q'))
        assertEquals(false, EditDistance.areNeighbours('q', 'p'))
    }

    @Test
    fun `suggestions are ordered by score`() {
        val suggestions = listOf(
            Suggestion("beta", 10.0, Suggestion.Source.COMPLETION),
            Suggestion("alpha", 90.0, Suggestion.Source.EXACT),
            Suggestion("gamma", 50.0, Suggestion.Source.PREDICTION)
        ).sortedByDescending { it.score }

        assertEquals(listOf("alpha", "gamma", "beta"), suggestions.map { it.word })
    }

    @Test
    fun `auto correction flag defaults to false`() {
        assertEquals(false, Suggestion("word", 1.0, Suggestion.Source.EXACT).isAutoCorrection)
    }
}
