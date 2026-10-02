package com.customboard.keyboard.keyboard

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.DeviceUtils
import com.customboard.keyboard.utils.dpToPx

/**
 * Floating keyboard: the keys detach from the bottom edge and can be dragged around the
 * input window with the grab handle, then resized with the same handle.
 */
class FloatingKeyboardManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    private var dragStartX = 0f
    private var dragStartY = 0f
    private var originX = 0
    private var originY = 0

    val isEnabled: Boolean get() = prefs.floatingMode

    fun toggle(): Boolean {
        val newValue = !prefs.floatingMode
        prefs.floatingMode = newValue
        return newValue
    }

    fun apply(container: View) {
        val params = container.layoutParams as? FrameLayout.LayoutParams ?: return
        if (!isEnabled) {
            params.width = FrameLayout.LayoutParams.MATCH_PARENT
            params.gravity = Gravity.BOTTOM
            params.leftMargin = 0
            params.bottomMargin = 0
        } else {
            val screenWidth = DeviceUtils.screenWidthPx(context)
            params.width = (screenWidth * 0.82f).toInt()
            params.gravity = Gravity.BOTTOM or Gravity.START
            params.leftMargin = prefs.floatingX.coerceIn(0, screenWidth - params.width)
            params.bottomMargin = prefs.floatingY.coerceIn(0, context.dpToPx(260f).toInt())
        }
        container.layoutParams = params
    }

    /** Hook the drag handle up to the container. */
    fun attachDragHandle(handle: View, container: View) {
        handle.setOnTouchListener { _, event ->
            if (!isEnabled) return@setOnTouchListener false
            val params = container.layoutParams as? FrameLayout.LayoutParams
                ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    originX = params.leftMargin
                    originY = params.bottomMargin
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - dragStartX).toInt()
                    val dy = (dragStartY - event.rawY).toInt()
                    val maxX = (DeviceUtils.screenWidthPx(context) - params.width).coerceAtLeast(0)
                    params.leftMargin = (originX + dx).coerceIn(0, maxX)
                    params.bottomMargin = (originY + dy).coerceIn(0, context.dpToPx(280f).toInt())
                    container.layoutParams = params
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    prefs.floatingX = params.leftMargin
                    prefs.floatingY = params.bottomMargin
                    handle.performClick()
                    true
                }

                else -> false
            }
        }
    }
}
