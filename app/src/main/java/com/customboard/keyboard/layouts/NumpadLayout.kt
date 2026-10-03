package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder
import com.customboard.keyboard.utils.KeyCodes

/** Calculator-style number pad shown for numeric input fields. */
object NumpadLayout {

    fun create(showAbcKey: Boolean = true): KeyboardLayout {
        val rows = listOf(
            KeyRow(
                listOf(
                    key("("), key(")"), key("%"), LayoutBuilder.deleteKey(1f)
                )
            ),
            KeyRow(listOf(key("7"), key("8"), key("9"), key("÷"))),
            KeyRow(listOf(key("4"), key("5"), key("6"), key("×"))),
            KeyRow(listOf(key("1"), key("2"), key("3"), key("−"))),
            KeyRow(
                listOf(
                    if (showAbcKey) LayoutBuilder.modeKey(KeyCodes.MODE_LETTERS, "ABC", 1f)
                    else key(","),
                    key("0"),
                    key("."),
                    LayoutBuilder.enterKey(1f)
                )
            )
        )
        return KeyboardLayout("numpad", rows, supportsShift = false, displayName = "Numbers")
    }

    private fun key(text: String): Key = Key(
        code = if (text.length == 1) text[0].code else 0,
        label = text,
        output = text,
        widthWeight = 1f
    )
}
