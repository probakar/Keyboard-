package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Arabic keyboard (right-to-left), standard 102-key arrangement. */
object ArabicLayout {

    private val POPUPS = mapOf(
        "ا" to listOf("أ", "إ", "آ", "ٱ"),
        "و" to listOf("ؤ"),
        "ي" to listOf("ئ", "ى"),
        "ه" to listOf("ة"),
        "ل" to listOf("لا", "لأ", "لإ", "لآ"),
        "ت" to listOf("ة"),
        "د" to listOf("ذ"),
        "ر" to listOf("ز"),
        "س" to listOf("ش"),
        "ص" to listOf("ض"),
        "ط" to listOf("ظ"),
        "ع" to listOf("غ")
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"), POPUPS
        )
        rows += LayoutBuilder.charRow(
            listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك"), POPUPS
        )
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(
                    LayoutBuilder.charRow(
                        listOf("ء", "ر", "ؤ", "ى", "ة", "و", "ز", "ظ"), POPUPS
                    ).keys
                )
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow(
            commaPopups = listOf("،", "؛", "؟", "!", ":", "\"", "'"),
            periodPopups = listOf(".", "،", "؟", "!", "…", ":")
        )
        return KeyboardLayout(
            id = "arabic",
            rows = rows,
            isRtl = true,
            languageTag = "ar",
            supportsShift = false,
            displayName = "العربية"
        )
    }
}
