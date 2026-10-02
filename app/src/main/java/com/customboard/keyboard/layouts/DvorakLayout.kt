package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Simplified Dvorak layout for English. */
object DvorakLayout {

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow("',.pyfgcrl")
        rows += LayoutBuilder.charRow("aoeuidhtns")
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(LayoutBuilder.charRow(";qjkxbmwvz").keys)
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow()
        return KeyboardLayout("dvorak", rows, languageTag = "en", displayName = "Dvorak")
    }
}
