package com.customboard.keyboard.gesture

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyCodes
import org.json.JSONObject

/**
 * Maps a gesture to a key code. Defaults match Gboard, and every binding can be overridden by
 * the user; the overrides are stored as a small JSON object in the preferences.
 */
class CustomGestureMapper(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    /** Gestures the user can rebind. */
    enum class Gesture(val id: String, val defaultAction: Int) {
        SWIPE_UP("swipe_up", KeyCodes.SHIFT),
        SWIPE_DOWN("swipe_down", KeyCodes.HIDE_KEYBOARD),
        SWIPE_LEFT("swipe_left", KeyCodes.DELETE_WORD),
        SWIPE_RIGHT("swipe_right", KeyCodes.NONE),
        TWO_FINGER_SWIPE_DOWN("two_down", KeyCodes.HIDE_KEYBOARD),
        TWO_FINGER_SWIPE_UP("two_up", KeyCodes.MODE_NUMBER_ROW),
        LONG_PRESS_SPACE("long_space", KeyCodes.LANGUAGE),
        LONG_PRESS_ENTER("long_enter", KeyCodes.NEWLINE);

        companion object {
            fun byId(id: String): Gesture? = values().firstOrNull { it.id == id }
        }
    }

    fun actionFor(gesture: Gesture): Int {
        val overrides = runCatching { JSONObject(prefs.gestureMap) }.getOrNull()
            ?: return gesture.defaultAction
        val value = overrides.optInt(gesture.id, Int.MIN_VALUE)
        return if (value == Int.MIN_VALUE) gesture.defaultAction else value
    }

    fun bind(gesture: Gesture, action: Int) {
        val overrides = runCatching { JSONObject(prefs.gestureMap) }.getOrDefault(JSONObject())
        overrides.put(gesture.id, action)
        prefs.gestureMap = overrides.toString()
    }

    fun reset(gesture: Gesture) {
        val overrides = runCatching { JSONObject(prefs.gestureMap) }.getOrNull() ?: return
        overrides.remove(gesture.id)
        prefs.gestureMap = overrides.toString()
    }

    fun resetAll() {
        prefs.gestureMap = "{}"
    }

    /** Current binding of every gesture, for the settings screen. */
    fun bindings(): Map<Gesture, Int> = Gesture.values().associateWith { actionFor(it) }
}
