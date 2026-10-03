package com.customboard.keyboard.accessibility

import android.content.Context
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.customboard.keyboard.R
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyCodes

/**
 * TalkBack support: spoken key names, announcements for mode changes and high contrast /
 * large text handling.
 */
class AccessibilityHelper(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    private val manager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    val isScreenReaderOn: Boolean
        get() = manager?.isEnabled == true && manager.isTouchExplorationEnabled

    val isEnabled: Boolean get() = manager?.isEnabled == true

    /** Human readable description of a key, used for TalkBack and for key popups. */
    fun describe(key: Key, uppercase: Boolean): String {
        key.description?.let { return it }
        return when (key.code) {
            KeyCodes.DELETE -> context.getString(R.string.key_delete)
            KeyCodes.ENTER -> context.getString(R.string.key_enter)
            KeyCodes.SHIFT -> context.getString(R.string.key_shift)
            KeyCodes.SPACE -> context.getString(R.string.key_space)
            KeyCodes.EMOJI -> context.getString(R.string.key_emoji)
            KeyCodes.LANGUAGE -> context.getString(R.string.key_language)
            KeyCodes.VOICE -> context.getString(R.string.key_voice)
            KeyCodes.SETTINGS -> context.getString(R.string.key_settings)
            KeyCodes.MODE_SYMBOLS, KeyCodes.MODE_SYMBOLS_2 -> context.getString(R.string.key_symbols)
            KeyCodes.MODE_LETTERS -> context.getString(R.string.key_letters)
            KeyCodes.HIDE_KEYBOARD -> context.getString(R.string.key_hide)
            else -> when {
                key.type == KeyType.CHARACTER && uppercase -> key.label.uppercase()
                else -> key.label
            }
        }
    }

    fun announce(view: View, text: CharSequence) {
        if (!isEnabled || text.isBlank()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            view.announceForAccessibility(text)
        } else {
            @Suppress("DEPRECATION")
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
            event.text.add(text)
            event.className = view.javaClass.name
            event.packageName = context.packageName
            manager?.sendAccessibilityEvent(event)
        }
    }

    fun announceKey(view: View, key: Key, uppercase: Boolean) {
        if (!isScreenReaderOn) return
        announce(view, describe(key, uppercase))
    }

    /** Minimum touch target recommended by the accessibility guidelines. */
    fun minimumTouchTargetDp(): Float = if (prefs.largeKeys) 56f else 48f

    val useHighContrast: Boolean get() = prefs.highContrast

    /** Long press delay multiplier: screen reader users get a little more time. */
    val holdDurationMultiplier: Float get() = if (isScreenReaderOn) 1.6f else 1f
}
