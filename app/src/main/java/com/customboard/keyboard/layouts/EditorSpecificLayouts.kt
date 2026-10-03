package com.customboard.keyboard.layouts

import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder
import com.customboard.keyboard.utils.KeyCodes

/** Reuses the active language's letter rows while adding high-value email/URL keys. */
object EditorSpecificLayouts {

    fun email(base: KeyboardLayout): KeyboardLayout = base.withBottomRow(
        id = "email_${base.id}",
        keys = listOf(
            LayoutBuilder.modeKey(KeyCodes.MODE_SYMBOLS, "?123", 1.35f),
            Key(code = '@'.code, label = "@", widthWeight = 1f),
            Key(code = ','.code, label = ",", widthWeight = 0.85f),
            LayoutBuilder.spaceKey(3.1f),
            Key(code = '.'.code, label = ".", widthWeight = 0.85f),
            Key(code = KeyCodes.NONE, label = ".com", output = ".com", widthWeight = 1.3f),
            LayoutBuilder.enterKey(1.55f)
        )
    )

    fun url(base: KeyboardLayout): KeyboardLayout = base.withBottomRow(
        id = "url_${base.id}",
        keys = listOf(
            LayoutBuilder.modeKey(KeyCodes.MODE_SYMBOLS, "?123", 1.3f),
            Key(code = '/'.code, label = "/", widthWeight = 0.85f),
            Key(code = '.'.code, label = ".", widthWeight = 0.85f),
            Key(code = KeyCodes.NONE, label = "www.", output = "www.", widthWeight = 1.25f),
            LayoutBuilder.spaceKey(2.65f),
            Key(code = KeyCodes.NONE, label = ".com", output = ".com", widthWeight = 1.35f),
            LayoutBuilder.enterKey(1.55f)
        )
    )

    private fun KeyboardLayout.withBottomRow(id: String, keys: List<Key>): KeyboardLayout = copy(
        id = id,
        rows = rows.dropLast(1) + KeyRow(keys),
        displayName = when {
            id.startsWith("email_") -> "$displayName · Email"
            else -> "$displayName · URL"
        }
    )
}
