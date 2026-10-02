package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Dialler pad used for phone number fields. */
object PhonepadLayout {

    fun create(): KeyboardLayout {
        val rows = listOf(
            KeyRow(listOf(key("1"), key("2", "ABC"), key("3", "DEF"), LayoutBuilder.deleteKey(1f))),
            KeyRow(listOf(key("4", "GHI"), key("5", "JKL"), key("6", "MNO"), key("-"))),
            KeyRow(listOf(key("7", "PQRS"), key("8", "TUV"), key("9", "WXYZ"), key("+"))),
            KeyRow(
                listOf(
                    key("*", popups = listOf("p", "w", "N")),
                    key("0", "+"),
                    key("#", popups = listOf(",", ";")),
                    LayoutBuilder.enterKey(1f)
                )
            )
        )
        return KeyboardLayout("phonepad", rows, supportsShift = false, displayName = "Phone")
    }

    private fun key(text: String, hint: String? = null, popups: List<String> = emptyList()): Key =
        Key(
            code = if (text.length == 1) text[0].code else 0,
            label = text,
            hint = hint,
            output = text,
            popupKeys = popups,
            widthWeight = 1f
        )
}
