package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Urdu keyboard (right-to-left) following the common phonetic arrangement. */
object UrduLayout {

    private val POPUPS = mapOf(
        "ا" to listOf("آ", "أ", "إ", "ء"),
        "و" to listOf("ؤ", "ۇ"),
        "ی" to listOf("ئ", "ي", "ے"),
        "ہ" to listOf("ھ", "ة", "ۀ"),
        "ک" to listOf("ك"),
        "ن" to listOf("ں"),
        "ر" to listOf("ڑ"),
        "د" to listOf("ڈ"),
        "ت" to listOf("ٹ", "ة"),
        "ز" to listOf("ژ", "ذ", "ض", "ظ"),
        "س" to listOf("ص", "ث"),
        "ح" to listOf("ہ", "ھ")
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            listOf("ق", "و", "ع", "ر", "ت", "ے", "ء", "ی", "ہ", "پ"), POPUPS
        )
        rows += LayoutBuilder.charRow(
            listOf("ا", "س", "د", "ف", "گ", "ح", "ج", "ک", "ل"), POPUPS
        )
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(
                    LayoutBuilder.charRow(
                        listOf("ز", "ش", "چ", "ط", "ب", "ن", "م"), POPUPS
                    ).keys
                )
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow(
            commaPopups = listOf("،", "؛", "؟", "!", ":", "\"", "'"),
            periodPopups = listOf("۔", "،", "؟", "!", "…", ":")
        )
        return KeyboardLayout(
            id = "urdu",
            rows = rows,
            isRtl = true,
            languageTag = "ur",
            supportsShift = false,
            displayName = "اردو"
        )
    }
}
