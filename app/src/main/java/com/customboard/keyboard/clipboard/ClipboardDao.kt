package com.customboard.keyboard.clipboard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_items ORDER BY is_pinned DESC, timestamp DESC")
    fun observeAll(): Flow<List<ClipboardEntity>>

    @Query("SELECT * FROM clipboard_items ORDER BY is_pinned DESC, timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ClipboardEntity>

    @Query(
        "SELECT * FROM clipboard_items WHERE content LIKE '%' || :query || '%' " +
            "ORDER BY is_pinned DESC, timestamp DESC"
    )
    suspend fun search(query: String): List<ClipboardEntity>

    @Query("SELECT * FROM clipboard_items WHERE category = :category ORDER BY is_pinned DESC, timestamp DESC")
    suspend fun byCategory(category: String): List<ClipboardEntity>

    @Query("SELECT * FROM clipboard_items WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): ClipboardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardEntity): Long

    @Update
    suspend fun update(item: ClipboardEntity)

    @Query("UPDATE clipboard_items SET is_pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE clipboard_items SET content = :content, preview = :preview WHERE id = :id")
    suspend fun updateContent(id: Long, content: String, preview: String)

    @Query("DELETE FROM clipboard_items WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM clipboard_items")
    suspend fun deleteAll()

    @Query("DELETE FROM clipboard_items WHERE is_pinned = 0")
    suspend fun deleteUnpinned()

    @Query("DELETE FROM clipboard_items WHERE is_pinned = 0 AND timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long): Int

    @Query("SELECT COUNT(*) FROM clipboard_items")
    suspend fun count(): Int

    @Query(
        "DELETE FROM clipboard_items WHERE id IN (SELECT id FROM clipboard_items " +
            "WHERE is_pinned = 0 ORDER BY timestamp DESC LIMIT -1 OFFSET :keep)"
    )
    suspend fun trimTo(keep: Int)
}
