package com.customboard.keyboard.gesture

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.dpToPx
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Converts a horizontal slide on the space bar into cursor steps.
 *
 * One step is emitted every [STEP_DP] density-independent pixels, and the step size grows once
 * the finger has travelled far enough so long slides stay fast without losing precision.
 */
class SpacebarGestureHandler(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val stepPx = context.dpToPx(STEP_DP)
    private val accelerateAfterPx = context.dpToPx(ACCELERATE_AFTER_DP)

    private var active = false
    private var emittedSteps = 0

    val isActive: Boolean get() = active

    /** Cumulative signed step count since the slide began. */
    val steps: Int get() = emittedSteps

    fun begin(): Boolean {
        if (!prefs.spaceCursor) return false
        active = true
        emittedSteps = 0
        return true
    }

    /**
     * @param deltaPx total horizontal travel since the slide began
     * @return the number of characters the cursor should move now (may be negative or zero)
     */
    fun update(deltaPx: Float): Int {
        if (!active) return 0
        val magnitude = abs(deltaPx)
        val base = (magnitude / stepPx).toInt()
        val boosted = if (magnitude > accelerateAfterPx) {
            base + ((magnitude - accelerateAfterPx) / stepPx * 0.75f).roundToInt()
        } else {
            base
        }
        val target = if (deltaPx < 0) -boosted else boosted
        val delta = target - emittedSteps
        emittedSteps = target
        return delta
    }

    fun end() {
        active = false
        emittedSteps = 0
    }

    private companion object {
        const val STEP_DP = 11f
        const val ACCELERATE_AFTER_DP = 90f
    }
}
