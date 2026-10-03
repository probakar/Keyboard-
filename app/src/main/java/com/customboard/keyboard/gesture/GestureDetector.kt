package com.customboard.keyboard.gesture

import kotlin.math.abs
import kotlin.math.hypot

/** The four cardinal swipe directions, plus [NONE] when the movement is just a tap. */
enum class SwipeDirection { NONE, UP, DOWN, LEFT, RIGHT }

/** A finished touch path, already classified. */
data class GestureInfo(
    val direction: SwipeDirection,
    val distancePx: Float,
    val durationMs: Long,
    val pointerCount: Int
) {
    /** Pixels per millisecond - used to tell a flick from a slow drag. */
    val velocity: Float get() = if (durationMs <= 0L) 0f else distancePx / durationMs

    val isFlick: Boolean get() = velocity > 0.6f
}

/**
 * Classifies raw touch paths into [GestureInfo].
 *
 * This is deliberately independent from `android.view.GestureDetector`: the keyboard needs the
 * dominant axis and the travelled distance of a path that may contain dozens of points (glide
 * typing), which the framework detector does not expose. Keeping points as plain Kotlin values
 * also makes the classifier deterministic in local JVM tests without Android framework stubs.
 */
class GestureDetector(private val thresholdPx: Float) {

    private data class Point(val x: Float, val y: Float)

    private val points = ArrayList<Point>(64)
    private var startTime = 0L
    private var pointerCount = 1

    fun begin(x: Float, y: Float, pointers: Int = 1) {
        points.clear()
        points.add(Point(x, y))
        startTime = System.currentTimeMillis()
        pointerCount = pointers
    }

    fun update(x: Float, y: Float, pointers: Int = 1) {
        points.add(Point(x, y))
        if (pointers > pointerCount) pointerCount = pointers
    }

    /** Total length of the path, not just the straight line between the ends. */
    fun pathLength(): Float {
        var total = 0f
        for (index in 1 until points.size) {
            total += hypot(
                points[index].x - points[index - 1].x,
                points[index].y - points[index - 1].y
            )
        }
        return total
    }

    fun end(): GestureInfo {
        if (points.size < 2) {
            return GestureInfo(SwipeDirection.NONE, 0f, 0L, pointerCount)
        }
        val first = points.first()
        val last = points.last()
        val dx = last.x - first.x
        val dy = last.y - first.y
        val distance = hypot(dx, dy)
        val duration = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)

        val direction = when {
            distance < thresholdPx -> SwipeDirection.NONE
            abs(dx) > abs(dy) -> if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
            else -> if (dy > 0) SwipeDirection.DOWN else SwipeDirection.UP
        }
        return GestureInfo(direction, distance, duration, pointerCount)
    }

    fun reset() {
        points.clear()
        pointerCount = 1
    }
}
