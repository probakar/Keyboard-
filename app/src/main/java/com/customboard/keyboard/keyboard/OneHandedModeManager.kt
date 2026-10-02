package com.customboard.keyboard.keyboard

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.DeviceUtils

/**
 * One-handed mode: the keyboard shrinks and sticks to the left or right edge so it can be
 * reached with a single thumb. The remaining space holds the shortcut rail.
 */
class OneHandedModeManager(private val context: Context) {

    enum class Side { OFF, LEFT, RIGHT }

    private val prefs = PreferencesManager.getInstance(context)

    val side: Side
        get() = when (prefs.oneHandedMode) {
            "left" -> Side.LEFT
            "right" -> Side.RIGHT
            else -> Side.OFF
        }

    val isEnabled: Boolean get() = side != Side.OFF

    fun toggle(): Side {
        val next = when (side) {
            Side.OFF -> Side.RIGHT
            Side.RIGHT -> Side.LEFT
            Side.LEFT -> Side.OFF
        }
        prefs.oneHandedMode = when (next) {
            Side.OFF -> "off"
            Side.LEFT -> "left"
            Side.RIGHT -> "right"
        }
        return next
    }

    fun setSide(newSide: Side) {
        prefs.oneHandedMode = when (newSide) {
            Side.OFF -> "off"
            Side.LEFT -> "left"
            Side.RIGHT -> "right"
        }
    }

    /** Applies the current mode to the keyboard container. */
    fun apply(container: View) {
        val params = container.layoutParams as? FrameLayout.LayoutParams ?: return
        if (!isEnabled) {
            params.width = FrameLayout.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.BOTTOM
            params.leftMargin = 0
            params.rightMargin = 0
        } else {
            val screenWidth = DeviceUtils.screenWidthPx(context)
            val scaled = (screenWidth * prefs.oneHandedScale / 100f).toInt()
            params.width = scaled
            params.gravity = when (side) {
                Side.LEFT -> Gravity.BOTTOM or Gravity.START
                else -> Gravity.BOTTOM or Gravity.END
            }
        }
        container.layoutParams = params
    }

    /** Width of the empty rail next to the keyboard, in pixels. */
    fun railWidthPx(): Int {
        if (!isEnabled) return 0
        val screenWidth = DeviceUtils.screenWidthPx(context)
        return screenWidth - (screenWidth * prefs.oneHandedScale / 100f).toInt()
    }
}
