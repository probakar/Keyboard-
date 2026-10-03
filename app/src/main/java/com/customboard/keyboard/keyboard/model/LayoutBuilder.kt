package com.customboard.keyboard.keyboard.model

import com.customboard.keyboard.R
import com.customboard.keyboard.utils.KeyCodes

/** Small helpers that keep the layout definitions declarative and short. */
object LayoutBuilder {

    /** Builds a row of character keys from a plain string, with optional popups and hints. */
    fun charRow(
        chars: String,
        popups: Map<Char, String> = emptyMap(),
        hints: String? = null,
        widthWeight: Float = 1f
    ): KeyRow {
        val keys = chars.mapIndexed { index, c ->
            Key(
                code = c.code,
                label = c.toString(),
                hint = hints?.getOrNull(index)?.toString(),
                popupKeys = popups[c]?.map { it.toString() } ?: emptyList(),
                widthWeight = widthWeight
            )
        }
        return KeyRow(keys)
    }

    /** Row of character keys where every popup is defined as a whole string. */
    fun charRow(chars: List<String>, popups: Map<String, List<String>> = emptyMap()): KeyRow {
        val keys = chars.map { text ->
            Key(
                code = if (text.length == 1) text[0].code else KeyCodes.NONE,
                label = text,
                output = text,
                popupKeys = popups[text] ?: emptyList()
            )
        }
        return KeyRow(keys)
    }

    fun shiftKey(weight: Float = 1.5f) = Key(
        code = KeyCodes.SHIFT,
        label = "",
        type = KeyType.SHIFT,
        iconRes = R.drawable.ic_shift,
        widthWeight = weight,
        description = "shift"
    )

    fun deleteKey(weight: Float = 1.5f) = Key(
        code = KeyCodes.DELETE,
        label = "",
        type = KeyType.DELETE,
        iconRes = R.drawable.ic_backspace,
        widthWeight = weight,
        repeatable = true,
        description = "delete"
    )

    fun enterKey(weight: Float = 1.65f) = Key(
        code = KeyCodes.ENTER,
        label = "",
        type = KeyType.ENTER,
        iconRes = R.drawable.ic_enter,
        widthWeight = weight,
        description = "enter"
    )

    fun spaceKey(weight: Float = 4.4f, label: String = "") = Key(
        code = KeyCodes.SPACE,
        label = label,
        type = KeyType.SPACE,
        widthWeight = weight,
        description = "space"
    )

    fun modeKey(code: Int, label: String, weight: Float = 1.5f) = Key(
        code = code,
        label = label,
        type = KeyType.MODIFIER,
        widthWeight = weight
    )

    fun functionKey(
        code: Int,
        iconRes: Int = 0,
        label: String = "",
        weight: Float = 1f,
        description: String? = null
    ) = Key(
        code = code,
        label = label,
        iconRes = iconRes,
        type = KeyType.FUNCTION,
        widthWeight = weight,
        description = description
    )

    fun emojiKey(weight: Float = 1f) =
        functionKey(KeyCodes.EMOJI, R.drawable.ic_emoji, weight = weight, description = "emoji")

    fun languageKey(weight: Float = 1f) =
        functionKey(KeyCodes.LANGUAGE, R.drawable.ic_language, weight = weight, description = "language")

    /** The standard Gboard-style bottom row. */
    fun bottomRow(
        symbolsLabel: String = "?123",
        commaPopups: List<String> = listOf("!", "?", ":", ";", "'", "\"", "-", "_", "@"),
        periodPopups: List<String> = listOf("!", "?", ",", ";", ":", "…", "'", "\"", "/"),
        showLanguageKey: Boolean = true
    ): KeyRow {
        val keys = mutableListOf<Key>()
        keys += modeKey(KeyCodes.MODE_SYMBOLS, symbolsLabel, 1.4f)
        keys += Key(
            code = ','.code,
            label = ",",
            popupKeys = commaPopups,
            widthWeight = 1f
        )
        if (showLanguageKey) keys += languageKey(1f) else keys += emojiKey(1f)
        keys += spaceKey(4.2f)
        keys += Key(
            code = '.'.code,
            label = ".",
            popupKeys = periodPopups,
            widthWeight = 1f
        )
        keys += enterKey(1.6f)
        return KeyRow(keys)
    }

    /** Digits row that can be prepended to any alphabetic layout. */
    fun numberRow(): KeyRow = charRow(
        "1234567890",
        popups = mapOf(
            '1' to "¹½⅓¼",
            '2' to "²⅔",
            '3' to "³¾⅜",
            '4' to "⁴",
            '0' to "⁰ⁿ∅"
        )
    )

    /** Hints shown above the top letter row when the number row is hidden. */
    const val TOP_ROW_HINTS = "1234567890"
}
