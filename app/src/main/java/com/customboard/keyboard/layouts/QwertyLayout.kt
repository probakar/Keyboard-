package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** English QWERTY (US / UK) with accent popups, matching the Gboard key arrangement. */
object QwertyLayout {

    private val POPUPS = mapOf(
        'q' to "q1",
        'w' to "w2",
        'e' to "èéêëēėę3",
        'r' to "r4",
        't' to "t5",
        'y' to "ÿ6",
        'u' to "ûüùúū7",
        'i' to "îïíīįì8",
        'o' to "ôöòóœøōõ9",
        'p' to "p0",
        'a' to "àáâäæãåā",
        's' to "śš",
        'd' to "d",
        'f' to "f",
        'g' to "g",
        'h' to "h",
        'j' to "j",
        'k' to "k",
        'l' to "ł",
        'z' to "žźż",
        'x' to "x",
        'c' to "çćč",
        'v' to "v",
        'b' to "b",
        'n' to "ñń",
        'm' to "m"
    )

    fun create(withNumberRow: Boolean = false, languageTag: String = "en_US"): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            "qwertyuiop",
            POPUPS,
            hints = if (withNumberRow) null else LayoutBuilder.TOP_ROW_HINTS
        )
        rows += KeyRow(LayoutBuilder.charRow("asdfghjkl", POPUPS).keys, 1f)
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(LayoutBuilder.charRow("zxcvbnm", POPUPS).keys)
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow()
        return KeyboardLayout(
            id = "qwerty_$languageTag",
            rows = rows,
            languageTag = languageTag,
            displayName = if (languageTag == "en_GB") "English (UK)" else "English (US)"
        )
    }
}
