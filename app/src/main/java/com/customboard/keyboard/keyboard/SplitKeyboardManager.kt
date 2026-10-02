package com.customboard.keyboard.keyboard

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.DeviceUtils

/** Split keyboard support (tablets and large landscape phones). */
class SplitKeyboardManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    /** Splitting is only sensible when the keyboard is wide enough. */
    val isAvailable: Boolean
        get() = DeviceUtils.isTablet(context) ||
            DeviceUtils.isLandscape(context) ||
            DeviceUtils.smallestWidthDp(context) >= 480

    val isEnabled: Boolean get() = prefs.splitKeyboard && isAvailable

    fun toggle(): Boolean {
        if (!isAvailable) return false
        val newValue = !prefs.splitKeyboard
        prefs.splitKeyboard = newValue
        return newValue
    }
}
