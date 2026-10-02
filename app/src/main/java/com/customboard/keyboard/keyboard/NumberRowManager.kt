package com.customboard.keyboard.keyboard

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/** Toggles the dedicated digits row above the letters. */
class NumberRowManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    val isEnabled: Boolean get() = prefs.numberRow

    /** @return the new state. */
    fun toggle(): Boolean {
        val newValue = !prefs.numberRow
        prefs.numberRow = newValue
        return newValue
    }

    fun setEnabled(enabled: Boolean) {
        prefs.numberRow = enabled
    }
}
