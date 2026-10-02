package com.customboard.keyboard.keyboard

import android.content.Context
import android.view.inputmethod.EditorInfo
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.KeyboardMode
import com.customboard.keyboard.keyboard.model.ShiftState
import com.customboard.keyboard.layouts.LayoutFactory
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyboardUtils
import org.json.JSONObject

/**
 * Owns the current page (letters / symbols / numpad), the active language and the shift state,
 * and produces the matching [KeyboardLayout].
 */
class KeyboardLayoutManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    var mode: KeyboardMode = KeyboardMode.LETTERS
        private set

    var shiftState: ShiftState = ShiftState.OFF
        private set

    var language: String = prefs.currentLanguage
        private set

    private var lastShiftTap = 0L

    fun currentLayout(): KeyboardLayout =
        LayoutFactory.create(language, mode, prefs.numberRow && mode == KeyboardMode.LETTERS)

    fun isRtl(): Boolean = LayoutFactory.isRtl(language)

    /** BCP-47 tag of the active layout, used for voice input and spell checking. */
    fun localeTag(): String = LayoutFactory.localeTag(language)

    fun spaceBarLabel(): String =
        if (prefs.enabledLanguages.size > 1) LayoutFactory.spaceBarLabel(language) else ""

    // ------------------------------------------------------------------
    //  Mode switching
    // ------------------------------------------------------------------

    fun setMode(newMode: KeyboardMode) {
        mode = newMode
        if (newMode != KeyboardMode.LETTERS) shiftState = ShiftState.OFF
    }

    fun toggleSymbols() {
        mode = when (mode) {
            KeyboardMode.LETTERS -> KeyboardMode.SYMBOLS
            else -> KeyboardMode.LETTERS
        }
    }

    /** Chooses the best page for the editor that just got focus. */
    fun adaptToEditor(info: EditorInfo?) {
        mode = when {
            KeyboardUtils.isPhoneField(info) -> KeyboardMode.PHONE
            KeyboardUtils.isNumberField(info) -> KeyboardMode.NUMPAD
            else -> KeyboardMode.LETTERS
        }
    }

    // ------------------------------------------------------------------
    //  Shift handling
    // ------------------------------------------------------------------

    /** Returns true when the tap resulted in caps lock (double tap). */
    fun onShiftTap(): Boolean {
        val now = System.currentTimeMillis()
        val isDoubleTap = now - lastShiftTap < 400L
        lastShiftTap = now
        shiftState = when {
            isDoubleTap -> ShiftState.LOCKED
            shiftState == ShiftState.OFF -> ShiftState.SHIFTED
            else -> ShiftState.OFF
        }
        return shiftState == ShiftState.LOCKED
    }

    fun setShift(state: ShiftState) {
        shiftState = state
    }

    /** Consumes a one-shot shift after a character was typed. */
    fun consumeShift() {
        if (shiftState == ShiftState.SHIFTED) shiftState = ShiftState.OFF
    }

    fun autoCapitalize(enable: Boolean) {
        if (shiftState == ShiftState.LOCKED) return
        shiftState = if (enable) ShiftState.SHIFTED else ShiftState.OFF
    }

    // ------------------------------------------------------------------
    //  Language switching
    // ------------------------------------------------------------------

    fun switchToNextLanguage(): String {
        val enabled = prefs.enabledLanguages
        language = LayoutFactory.nextLanguage(language, enabled)
        prefs.currentLanguage = language
        return LayoutFactory.displayName(language)
    }

    fun setLanguage(code: String) {
        language = code
        prefs.currentLanguage = code
    }

    fun refreshLanguageFromPrefs() {
        val stored = prefs.currentLanguage
        if (stored != language && prefs.enabledLanguages.contains(stored)) {
            language = stored
        }
        if (!prefs.enabledLanguages.contains(language)) {
            language = prefs.enabledLanguages.firstOrNull() ?: "en_US"
            prefs.currentLanguage = language
        }
    }

    // ------------------------------------------------------------------
    //  Per-app layout memory
    // ------------------------------------------------------------------

    fun rememberLayoutForApp(packageName: String?) {
        if (!prefs.rememberLayoutPerApp || packageName.isNullOrEmpty()) return
        val json = runCatching { JSONObject(prefs.layoutPerApp) }.getOrDefault(JSONObject())
        json.put(packageName, language)
        prefs.layoutPerApp = json.toString()
    }

    fun restoreLayoutForApp(packageName: String?) {
        if (!prefs.rememberLayoutPerApp || packageName.isNullOrEmpty()) return
        val json = runCatching { JSONObject(prefs.layoutPerApp) }.getOrNull() ?: return
        val stored = json.optString(packageName, "")
        if (stored.isNotEmpty() && prefs.enabledLanguages.contains(stored)) {
            language = stored
            prefs.currentLanguage = stored
        }
    }
}
