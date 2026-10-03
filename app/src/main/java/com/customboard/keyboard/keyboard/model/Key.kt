package com.customboard.keyboard.keyboard.model

import androidx.annotation.DrawableRes
import com.customboard.keyboard.utils.KeyCodes

/** What a key does, which drives its colour and behaviour. */
enum class KeyType {
    CHARACTER,
    FUNCTION,
    MODIFIER,
    SPACE,
    ENTER,
    DELETE,
    SHIFT
}

/**
 * A single key. Keys are immutable descriptions - all drawing state lives in the view.
 *
 * @param code       internal code (see [KeyCodes]); positive values are code points
 * @param label      what is drawn on the key
 * @param hint       small label drawn in the upper-right corner (long-press hint)
 * @param popupKeys  characters offered in the long-press popup
 * @param widthWeight relative width inside its row (1 = one unit)
 */
data class Key(
    val code: Int,
    val label: String = "",
    val hint: String? = null,
    val popupKeys: List<String> = emptyList(),
    val widthWeight: Float = 1f,
    val type: KeyType = KeyType.CHARACTER,
    @DrawableRes val iconRes: Int = 0,
    val repeatable: Boolean = false,
    val output: String? = null,
    val description: String? = null
) {
    /** Text inserted when the key is tapped. */
    val outputText: String
        get() = output ?: if (code > 0) String(Character.toChars(code)) else label

    val isCharacter: Boolean get() = type == KeyType.CHARACTER && code > 0

    val isLetter: Boolean get() = isCharacter && label.isNotEmpty() && label[0].isLetter()
}

/** One row of keys. [heightWeight] allows taller rows (e.g. a numpad). */
data class KeyRow(
    val keys: List<Key>,
    val heightWeight: Float = 1f
) {
    val totalWeight: Float get() = keys.sumOf { it.widthWeight.toDouble() }.toFloat()
}

/** A complete keyboard page. */
data class KeyboardLayout(
    val id: String,
    val rows: List<KeyRow>,
    val isRtl: Boolean = false,
    val languageTag: String = "en",
    val supportsShift: Boolean = true,
    val displayName: String = id
) {
    val rowCount: Int get() = rows.size
    val totalHeightWeight: Float get() = rows.sumOf { it.heightWeight.toDouble() }.toFloat()
}

/** Which page of the keyboard is visible. */
enum class KeyboardMode {
    LETTERS,
    SYMBOLS,
    SYMBOLS_2,
    NUMPAD,
    PHONE
}

/** Shift state machine: off -> shifted (one shot) -> caps lock. */
enum class ShiftState {
    OFF,
    SHIFTED,
    LOCKED;

    val isUppercase: Boolean get() = this != OFF
}
