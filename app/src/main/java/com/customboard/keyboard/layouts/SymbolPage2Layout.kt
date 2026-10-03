package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder
import com.customboard.keyboard.utils.KeyCodes

/** Symbols page 2 - maths, currency and typographic symbols. */
object SymbolPage2Layout {

    private val POPUPS = mapOf(
        "€" to listOf("¢", "£", "¥", "₹", "₨", "₩", "₽", "₺", "₴"),
        "£" to listOf("¢", "€", "¥", "₹"),
        "¥" to listOf("¢", "€", "£"),
        "°" to listOf("℃", "℉"),
        "{" to listOf("[", "(", "<"),
        "}" to listOf("]", ")", ">"),
        "\\" to listOf("|", "/"),
        "©" to listOf("℗"),
        "™" to listOf("℠"),
        "•" to listOf("‣", "◦", "·")
    )

    fun create(): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        rows += LayoutBuilder.charRow(
            listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆"), POPUPS
        )
        rows += LayoutBuilder.charRow(
            listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\"), POPUPS
        )
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.modeKey(KeyCodes.MODE_SYMBOLS, "?123", 1.5f))
                addAll(
                    LayoutBuilder.charRow(
                        listOf("%", "©", "®", "™", "✓", "[", "]"), POPUPS
                    ).keys
                )
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += KeyRow(
            listOf(
                LayoutBuilder.modeKey(KeyCodes.MODE_LETTERS, "ABC", 1.4f),
                Key(code = '<'.code, label = "<", popupKeys = listOf("≤", "«")),
                LayoutBuilder.emojiKey(),
                LayoutBuilder.spaceKey(4.2f),
                Key(code = '>'.code, label = ">", popupKeys = listOf("≥", "»")),
                LayoutBuilder.enterKey(1.6f)
            )
        )
        return KeyboardLayout("symbols_2", rows, supportsShift = false, displayName = "Symbols 2")
    }
}
