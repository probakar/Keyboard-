package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** French AZERTY. */
object AzertyLayout {

    private val POPUPS = mapOf(
        'a' to "àâáäãå",
        'z' to "z",
        'e' to "éèêë",
        'r' to "r",
        't' to "t",
        'y' to "ÿü",
        'u' to "ùûü",
        'i' to "îï",
        'o' to "ôœöò",
        'p' to "p",
        'q' to "q",
        's' to "ß",
        'd' to "d",
        'f' to "f",
        'g' to "g",
        'h' to "h",
        'j' to "j",
        'k' to "k",
        'l' to "l",
        'm' to "m",
        'w' to "w",
        'x' to "x",
        'c' to "çć",
        'v' to "v",
        'b' to "b",
        'n' to "ñ"
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            "azertyuiop", POPUPS,
            hints = if (withNumberRow) null else LayoutBuilder.TOP_ROW_HINTS
        )
        rows += LayoutBuilder.charRow("qsdfghjklm", POPUPS)
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(LayoutBuilder.charRow("wxcvbn", POPUPS).keys)
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow()
        return KeyboardLayout("azerty_fr", rows, languageTag = "fr", displayName = "Français")
    }
}
