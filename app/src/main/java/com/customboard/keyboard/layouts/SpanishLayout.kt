package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Spanish QWERTY including Ñ. */
object SpanishLayout {

    private val POPUPS = mapOf(
        'e' to "éèêë",
        'a' to "áàâä",
        'i' to "íìîï",
        'o' to "óòôö",
        'u' to "úùûü",
        'n' to "ñ",
        'c' to "ç",
        's' to "ś"
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            "qwertyuiop", POPUPS,
            hints = if (withNumberRow) null else LayoutBuilder.TOP_ROW_HINTS
        )
        rows += LayoutBuilder.charRow("asdfghjklñ", POPUPS)
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(LayoutBuilder.charRow("zxcvbnm", POPUPS).keys)
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow(
            periodPopups = listOf("!", "?", "¿", "¡", ",", ";", ":", "…")
        )
        return KeyboardLayout("qwerty_es", rows, languageTag = "es", displayName = "Español")
    }
}
