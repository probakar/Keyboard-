package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.KeyboardMode

/** Single entry point that maps (language, mode) to a concrete [KeyboardLayout]. */
object LayoutFactory {

    /** Every language the keyboard ships with, in picker order. */
    val SUPPORTED_LANGUAGES = listOf(
        "en_US", "en_GB", "ur", "ar", "hi", "fr", "de", "es", "dvorak"
    )

    fun displayName(languageCode: String): String = when (languageCode) {
        "en_US" -> "English (US)"
        "en_GB" -> "English (UK)"
        "ur" -> "اردو"
        "ar" -> "العربية"
        "hi" -> "हिन्दी"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "es" -> "Español"
        "dvorak" -> "Dvorak"
        else -> languageCode
    }

    /** Short label drawn on the space bar. */
    fun spaceBarLabel(languageCode: String): String = when (languageCode) {
        "en_US" -> "English (US)"
        "en_GB" -> "English (UK)"
        "ur" -> "اردو"
        "ar" -> "العربية"
        "hi" -> "हिन्दी"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "es" -> "Español"
        "dvorak" -> "Dvorak"
        else -> "English"
    }

    fun isRtl(languageCode: String): Boolean = languageCode == "ur" || languageCode == "ar"

    /** Locale used by the dictionary / spell checker for a keyboard language. */
    fun localeTag(languageCode: String): String = when (languageCode) {
        "en_US", "dvorak" -> "en"
        "en_GB" -> "en"
        else -> languageCode
    }

    fun create(
        languageCode: String,
        mode: KeyboardMode,
        withNumberRow: Boolean = false
    ): KeyboardLayout = when (mode) {
        KeyboardMode.SYMBOLS -> SymbolLayout.create()
        KeyboardMode.SYMBOLS_2 -> SymbolPage2Layout.create()
        KeyboardMode.NUMPAD -> NumpadLayout.create()
        KeyboardMode.PHONE -> PhonepadLayout.create()
        KeyboardMode.LETTERS -> letters(languageCode, withNumberRow)
    }

    private fun letters(languageCode: String, withNumberRow: Boolean): KeyboardLayout =
        when (languageCode) {
            "ur" -> UrduLayout.create(withNumberRow)
            "ar" -> ArabicLayout.create(withNumberRow)
            "hi" -> HindiLayout.create(withNumberRow)
            "fr" -> AzertyLayout.create(withNumberRow)
            "de" -> QwertzLayout.create(withNumberRow)
            "es" -> SpanishLayout.create(withNumberRow)
            "dvorak" -> DvorakLayout.create(withNumberRow)
            "en_GB" -> QwertyLayout.create(withNumberRow, "en_GB")
            else -> QwertyLayout.create(withNumberRow, "en_US")
        }

    /** Next language in the user's rotation (globe key / space bar swipe). */
    fun nextLanguage(current: String, enabled: Collection<String>): String {
        val list = SUPPORTED_LANGUAGES.filter { enabled.contains(it) }
        if (list.isEmpty()) return "en_US"
        val index = list.indexOf(current)
        return list[(index + 1 + list.size) % list.size]
    }
}
