package com.customboard.keyboard.gesture

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyCodes

/**
 * Turns a classified [GestureInfo] into the key code the keyboard should run, honouring both the
 * user's gesture bindings and the individual on/off switches in settings.
 */
class SwipeGestureHandler(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val mapper = CustomGestureMapper(context)

    /**
     * @param info     the finished gesture
     * @param keyHint  secondary character of the key the gesture started on, if any
     * @return the key code to execute, [KeyCodes.NONE] when the gesture is disabled
     */
    fun resolve(info: GestureInfo, keyHint: String? = null): Int {
        if (info.direction == SwipeDirection.NONE) return KeyCodes.NONE

        if (info.pointerCount >= 2) {
            return when (info.direction) {
                SwipeDirection.DOWN ->
                    mapper.actionFor(CustomGestureMapper.Gesture.TWO_FINGER_SWIPE_DOWN)

                SwipeDirection.UP ->
                    mapper.actionFor(CustomGestureMapper.Gesture.TWO_FINGER_SWIPE_UP)

                else -> KeyCodes.NONE
            }
        }

        return when (info.direction) {
            SwipeDirection.DOWN -> {
                if (!prefs.swipeDownToHide) KeyCodes.NONE
                else mapper.actionFor(CustomGestureMapper.Gesture.SWIPE_DOWN)
            }

            SwipeDirection.UP -> {
                // A key with a secondary character inserts it instead of running an action.
                if (prefs.swipeUpForSymbols && !keyHint.isNullOrEmpty()) KeyCodes.NONE
                else mapper.actionFor(CustomGestureMapper.Gesture.SWIPE_UP)
            }

            SwipeDirection.LEFT -> {
                if (!prefs.swipeDeleteWord) KeyCodes.NONE
                else mapper.actionFor(CustomGestureMapper.Gesture.SWIPE_LEFT)
            }

            SwipeDirection.RIGHT -> mapper.actionFor(CustomGestureMapper.Gesture.SWIPE_RIGHT)
            SwipeDirection.NONE -> KeyCodes.NONE
        }
    }

    /** True when a swipe up on this key should insert [keyHint] rather than run an action. */
    fun insertsHint(info: GestureInfo, keyHint: String?): Boolean =
        info.direction == SwipeDirection.UP && prefs.swipeUpForSymbols && !keyHint.isNullOrEmpty()
}
