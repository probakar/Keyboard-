package com.customboard.keyboard.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.customboard.keyboard.R
import com.customboard.keyboard.utils.KeyCodes

/** One button of the scrollable toolbar above the keys. */
data class ToolbarItem(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val action: Int,
    val isToggle: Boolean = false
) {
    companion object {
        val ALL: List<ToolbarItem> = listOf(
            ToolbarItem("ai", R.string.toolbar_ai, R.drawable.ic_ai, KeyCodes.AI_TOOLS),
            ToolbarItem("clipboard", R.string.toolbar_clipboard, R.drawable.ic_clipboard, KeyCodes.CLIPBOARD),
            ToolbarItem("emoji", R.string.toolbar_emoji, R.drawable.ic_emoji, KeyCodes.EMOJI),
            ToolbarItem("voice", R.string.toolbar_voice, R.drawable.ic_mic, KeyCodes.VOICE),
            ToolbarItem("gif", R.string.toolbar_gif, R.drawable.ic_gif, KeyCodes.GIF),
            ToolbarItem("sticker", R.string.toolbar_sticker, R.drawable.ic_sticker, KeyCodes.STICKER),
            ToolbarItem("translate", R.string.toolbar_translate, R.drawable.ic_translate, KeyCodes.TRANSLATE),
            ToolbarItem("search", R.string.toolbar_search, R.drawable.ic_search, KeyCodes.SEARCH),
            ToolbarItem("theme", R.string.toolbar_theme, R.drawable.ic_palette, KeyCodes.THEME),
            ToolbarItem("text_tools", R.string.toolbar_text_tools, R.drawable.ic_text_tools, KeyCodes.TEXT_TOOLS),
            ToolbarItem("handwriting", R.string.toolbar_handwriting, R.drawable.ic_edit, KeyCodes.HANDWRITING),
            ToolbarItem("tab", R.string.toolbar_tab, R.drawable.ic_enter, KeyCodes.INSERT_TAB),
            ToolbarItem("cursor", R.string.toolbar_cursor, R.drawable.ic_cursor_control, KeyCodes.SELECT_MODE),
            ToolbarItem("select_all", R.string.toolbar_select_all, R.drawable.ic_select_all, KeyCodes.SELECT_ALL),
            ToolbarItem("copy", R.string.toolbar_copy, R.drawable.ic_copy, KeyCodes.COPY),
            ToolbarItem("cut", R.string.toolbar_cut, R.drawable.ic_cut, KeyCodes.CUT),
            ToolbarItem("paste", R.string.toolbar_paste, R.drawable.ic_paste, KeyCodes.PASTE),
            ToolbarItem("undo", R.string.toolbar_undo, R.drawable.ic_undo, KeyCodes.UNDO),
            ToolbarItem("redo", R.string.toolbar_redo, R.drawable.ic_redo, KeyCodes.REDO),
            ToolbarItem("one_handed", R.string.toolbar_one_handed, R.drawable.ic_one_handed, KeyCodes.ONE_HANDED, true),
            ToolbarItem("floating", R.string.toolbar_floating, R.drawable.ic_floating, KeyCodes.FLOATING, true),
            ToolbarItem("split", R.string.toolbar_split, R.drawable.ic_split, KeyCodes.SPLIT, true),
            ToolbarItem("resize", R.string.toolbar_resize, R.drawable.ic_resize, KeyCodes.RESIZE),
            ToolbarItem("number_row", R.string.toolbar_number_row, R.drawable.ic_number_row, KeyCodes.MODE_NUMBER_ROW, true),
            ToolbarItem("incognito", R.string.toolbar_incognito, R.drawable.ic_incognito, KeyCodes.INCOGNITO, true),
            ToolbarItem("contacts", R.string.toolbar_contacts, R.drawable.ic_contacts, KeyCodes.CONTACTS),
            ToolbarItem("settings", R.string.toolbar_settings, R.drawable.ic_settings, KeyCodes.SETTINGS)
        )

        fun byId(id: String): ToolbarItem? = ALL.firstOrNull { it.id == id }
    }
}
