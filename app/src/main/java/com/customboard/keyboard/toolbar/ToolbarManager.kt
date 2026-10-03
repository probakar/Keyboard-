package com.customboard.keyboard.toolbar

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/**
 * Keeps the order and visibility of the toolbar buttons. The user can reorder and hide them
 * in settings; the result is stored as a simple ordered id list.
 */
class ToolbarManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    val isEnabled: Boolean get() = prefs.toolbarEnabled

    /** Items currently shown, in order. */
    fun visibleItems(): List<ToolbarItem> {
        val order = prefs.toolbarItems
        if (order.isEmpty()) return defaultItems()
        return order.mapNotNull { ToolbarItem.byId(it) }.ifEmpty { defaultItems() }
    }

    fun hiddenItems(): List<ToolbarItem> {
        val visible = visibleItems().map { it.id }.toSet()
        return ToolbarItem.ALL.filterNot { visible.contains(it.id) }
    }

    fun setOrder(ids: List<String>) {
        prefs.toolbarItems = ids.filter { ToolbarItem.byId(it) != null }
    }

    fun show(id: String) {
        if (visibleItems().any { it.id == id }) return
        setOrder(visibleItems().map { it.id } + id)
    }

    fun hide(id: String) {
        setOrder(visibleItems().map { it.id }.filterNot { it == id })
    }

    fun move(from: Int, to: Int) {
        val items = visibleItems().map { it.id }.toMutableList()
        if (from !in items.indices || to !in items.indices) return
        items.add(to, items.removeAt(from))
        setOrder(items)
    }

    fun resetToDefault() {
        setOrder(DEFAULT_ORDER)
    }

    private fun defaultItems(): List<ToolbarItem> = DEFAULT_ORDER.mapNotNull { ToolbarItem.byId(it) }

    companion object {
        val DEFAULT_ORDER = listOf(
            "ai", "clipboard", "emoji", "handwriting", "voice", "translate", "text_tools",
            "cursor", "sticker", "gif", "search", "theme", "floating", "one_handed",
            "split", "incognito", "settings", "tab"
        )
    }
}
