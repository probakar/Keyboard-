package com.customboard.keyboard.layouts

import com.customboard.keyboard.R
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyRow
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.keyboard.model.KeyboardLayout
import com.customboard.keyboard.keyboard.model.LayoutBuilder
import com.customboard.keyboard.utils.KeyCodes

/**
 * The action row shown underneath the emoji grid (ABC / search / space / backspace / enter).
 */
object EmojiLayout {

    fun bottomRow(): KeyRow = KeyRow(
        listOf(
            Key(
                code = KeyCodes.MODE_LETTERS,
                label = "ABC",
                type = KeyType.MODIFIER,
                widthWeight = 1.4f
            ),
            Key(
                code = KeyCodes.SEARCH,
                iconRes = R.drawable.ic_search,
                type = KeyType.FUNCTION,
                widthWeight = 1f,
                description = "search emoji"
            ),
            LayoutBuilder.spaceKey(4.2f),
            Key(
                code = KeyCodes.DELETE,
                iconRes = R.drawable.ic_backspace,
                type = KeyType.DELETE,
                widthWeight = 1.4f,
                repeatable = true,
                description = "delete"
            ),
            LayoutBuilder.enterKey(1.4f)
        )
    )

    fun create(): KeyboardLayout =
        KeyboardLayout("emoji_actions", listOf(bottomRow()), supportsShift = false)
}
