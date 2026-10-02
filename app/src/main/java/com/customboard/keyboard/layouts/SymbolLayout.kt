package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder
import com.customboard.keyboard.utils.KeyCodes

/** Symbols page 1 - digits and the most common punctuation. */
object SymbolLayout {

    private val POPUPS = mapOf(
        "1" to listOf("¹", "½", "⅓", "¼"),
        "2" to listOf("²", "⅔"),
        "3" to listOf("³", "¾", "⅜"),
        "0" to listOf("⁰", "ⁿ", "∅"),
        "-" to listOf("–", "—", "·", "_"),
        "+" to listOf("±", "⁺"),
        "(" to listOf("[", "{", "<"),
        ")" to listOf("]", "}", ">"),
        "/" to listOf("\\", "|", "÷"),
        "$" to listOf("¢", "€", "£", "¥", "₹", "₨", "₩", "₽"),
        "&" to listOf("§", "¶"),
        "\"" to listOf("“", "”", "„", "«", "»"),
        "'" to listOf("‘", "’", "‚", "‹", "›"),
        "!" to listOf("¡"),
        "?" to listOf("¿", "‽"),
        "%" to listOf("‰", "℅"),
        "#" to listOf("№"),
        "*" to listOf("†", "‡", "★", "☆"),
        ":" to listOf("∶"),
        "=" to listOf("≈", "≠", "≤", "≥")
    )

    fun create(): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        rows += LayoutBuilder.charRow(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), POPUPS
        )
        rows += LayoutBuilder.charRow(
            listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/"), POPUPS
        )
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.modeKey(KeyCodes.MODE_SYMBOLS_2, "=\\<", 1.5f))
                addAll(
                    LayoutBuilder.charRow(
                        listOf("*", "\"", "'", ":", ";", "!", "?"), POPUPS
                    ).keys
                )
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += KeyRow(
            listOf(
                LayoutBuilder.modeKey(KeyCodes.MODE_LETTERS, "ABC", 1.4f),
                Key(code = ','.code, label = ",", popupKeys = listOf("、", "¸")),
                Key(
                    code = KeyCodes.EMOJI, label = "", type = KeyType.FUNCTION,
                    iconRes = com.customboard.keyboard.R.drawable.ic_emoji
                ),
                LayoutBuilder.spaceKey(4.2f),
                Key(code = '.'.code, label = ".", popupKeys = listOf("…", "·")),
                LayoutBuilder.enterKey(1.6f)
            )
        )
        return KeyboardLayout("symbols", rows, supportsShift = false, displayName = "Symbols")
    }
}
