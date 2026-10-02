package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder

/** Hindi (Devanagari) keyboard based on the InScript arrangement. */
object HindiLayout {

    private val POPUPS = mapOf(
        "क" to listOf("क़", "ख", "ख़"),
        "ग" to listOf("ग़", "घ"),
        "ज" to listOf("ज़", "झ"),
        "ड" to listOf("ड़", "ढ", "ढ़"),
        "त" to listOf("थ"),
        "द" to listOf("ध"),
        "प" to listOf("फ", "फ़"),
        "ब" to listOf("भ"),
        "च" to listOf("छ"),
        "ट" to listOf("ठ"),
        "स" to listOf("श", "ष"),
        "र" to listOf("ऋ", "ॠ"),
        "ा" to listOf("ऑ", "ॉ"),
        "ी" to listOf("ि"),
        "ू" to listOf("ु")
    )

    fun create(withNumberRow: Boolean = false): KeyboardLayout {
        val rows = mutableListOf<KeyRow>()
        if (withNumberRow) rows += LayoutBuilder.numberRow()
        rows += LayoutBuilder.charRow(
            listOf("ौ", "ै", "ा", "ी", "ू", "ब", "ह", "ग", "द", "ज", "ड"), POPUPS
        )
        rows += LayoutBuilder.charRow(
            listOf("ो", "े", "्", "ि", "ु", "प", "र", "क", "त", "च", "ट"), POPUPS
        )
        rows += KeyRow(
            buildList {
                add(LayoutBuilder.shiftKey())
                addAll(
                    LayoutBuilder.charRow(
                        listOf("ं", "म", "न", "व", "ल", "स", "य", "भ"), POPUPS
                    ).keys
                )
                add(LayoutBuilder.deleteKey())
            }
        )
        rows += LayoutBuilder.bottomRow(
            periodPopups = listOf("।", "?", "!", ",", ";", ":", "…")
        )
        return KeyboardLayout(
            id = "hindi",
            rows = rows,
            languageTag = "hi",
            supportsShift = false,
            displayName = "हिन्दी"
        )
    }
}
