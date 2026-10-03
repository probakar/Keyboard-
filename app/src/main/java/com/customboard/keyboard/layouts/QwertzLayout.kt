package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** German QWERTZ with umlauts. */
object QwertzLayout {

    private val POPUPS = mapOf(
        'q' to "q",
        'w' to "w",
        'e' to "€éèê",
        'r' to "r",
        't' to "t",
        'z' to "z",
        'u' to "üùû",
        'i' to "ïî",
        'o' to "öòô",
        'p' to "p",
        'a' to "äàâ",
        's' to "ßś",
        'd' to "d",
        'f' to "f",
        'g' to "g",
        'h' to "h",
        'j' to "j",
        'k' to "k",
        'l' to "l",
        'y' to "y",
        'x' to "x",
        'c' to "ç",
        'v' to "v",
        'b' to "b",
        'n' to "ñ",
        'm' to "m"
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            "qwertzuiopü", POPUPS,
            hints = if (withNumberRow) null else LayoutBuilder.TOP_ROW_HINTS
        )
        rows += LayoutBuilder.charRow("asdfghjklöä", POPUPS)
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(LayoutBuilder.charRow("yxcvbnm", POPUPS).keys)
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow()
        return KeyboardLayout("qwertz_de", rows, languageTag = "de", displayName = "Deutsch")
    }
}
