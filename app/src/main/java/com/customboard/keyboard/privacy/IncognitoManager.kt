package com.customboard.keyboard.privacy

import android.content.Context
import android.view.inputmethod.EditorInfo
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyboardUtils

/**
 * Decides when the keyboard must behave privately: either because the user turned incognito
 * on, or because the field itself is sensitive (password, PIN, no-personalised-learning flag).
 */
class IncognitoManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    private var fieldPrivate = false

    /** True when nothing may be learned, stored or sent. */
    val isActive: Boolean get() = prefs.incognito || fieldPrivate

    val isUserToggled: Boolean get() = prefs.incognito

    fun toggle(): Boolean {
        prefs.incognito = !prefs.incognito
        return prefs.incognito
    }

    fun set(enabled: Boolean) {
        prefs.incognito = enabled
    }

    /** Re-evaluates the field flags every time the keyboard is attached to a new editor. */
    fun onStartInput(editorInfo: EditorInfo?) {
        fieldPrivate = when {
            editorInfo == null -> false
            KeyboardUtils.isPasswordField(editorInfo) -> true
            KeyboardUtils.isNoSuggestionField(editorInfo) -> true
            (editorInfo.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0 -> true
            else -> false
        }
    }

    /** True when the field itself forced private mode (shows the shield badge on the space bar). */
    val isFieldEnforced: Boolean get() = fieldPrivate
}
