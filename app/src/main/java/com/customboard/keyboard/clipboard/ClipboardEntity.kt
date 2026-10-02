package com.customboard.keyboard.clipboard

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One stored clip.
 *
 * ```sql
 * CREATE TABLE clipboard_items (
 *     id INTEGER PRIMARY KEY AUTOINCREMENT,
 *     content TEXT NOT NULL,
 *     timestamp LONG NOT NULL,
 *     is_pinned BOOLEAN DEFAULT 0,
 *     category TEXT DEFAULT 'text',
 *     preview TEXT
 * )
 * ```
 */
@Entity(tableName = "clipboard_items")
data class ClipboardEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "category")
    val category: String = CATEGORY_TEXT,

    @ColumnInfo(name = "preview")
    val preview: String = content.take(140)
) {
    companion object {
        const val CATEGORY_TEXT = "text"
        const val CATEGORY_LINK = "link"
        const val CATEGORY_NUMBER = "number"
        const val CATEGORY_EMAIL = "email"
        const val CATEGORY_PHONE = "phone"
        const val CATEGORY_CODE = "code"
        const val CATEGORY_ADDRESS = "address"
    }
}
