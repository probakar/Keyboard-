package com.customboard.keyboard.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureDetectorTest {

    private fun detector() = GestureDetector(thresholdPx = 20f)

    private fun swipe(dx: Float, dy: Float): SwipeDirection {
        val detector = detector()
        detector.begin(0f, 0f)
        detector.update(dx, dy)
        return detector.end().direction
    }

    @Test
    fun `a short tap is not a swipe`() {
        val detector = detector()
        detector.begin(10f, 10f)
        detector.update(12f, 11f)
        assertEquals(SwipeDirection.NONE, detector.end().direction)
    }

    @Test
    fun `horizontal movement wins over a small vertical drift`() {
        val detector = detector()
        detector.begin(0f, 0f)
        detector.update(120f, 8f)
        assertEquals(SwipeDirection.RIGHT, detector.end().direction)
    }

    @Test
    fun `all four directions are detected`() {
        assertEquals(SwipeDirection.LEFT, swipe(-100f, 0f))
        assertEquals(SwipeDirection.RIGHT, swipe(100f, 0f))
        assertEquals(SwipeDirection.UP, swipe(0f, -100f))
        assertEquals(SwipeDirection.DOWN, swipe(0f, 100f))
    }

    @Test
    fun `path length follows the curve, not the straight line`() {
        val detector = detector()
        detector.begin(0f, 0f)
        detector.update(0f, 100f)
        detector.update(0f, 0f)
        assertEquals(200f, detector.pathLength(), 0.5f)
    }

    @Test
    fun `reset clears the recorded path`() {
        val detector = detector()
        detector.begin(0f, 0f)
        detector.update(50f, 50f)
        detector.reset()
        assertTrue(detector.path().isEmpty())
    }

    @Test
    fun `velocity is zero for an instant gesture`() {
        val info = GestureInfo(SwipeDirection.UP, 100f, 0L, 1)
        assertEquals(0f, info.velocity, 0.0001f)
    }
}
