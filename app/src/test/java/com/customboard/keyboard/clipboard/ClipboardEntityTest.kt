package com.customboard.keyboard.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardEntityTest {

    @Test
    fun `preview is clipped to the maximum length`() {
        val entity = ClipboardEntity(content = "x".repeat(500))
        assertTrue(entity.preview.length <= 140)
    }

    @Test
    fun `short content is previewed verbatim`() {
        val entity = ClipboardEntity(content = "hello")
        assertEquals("hello", entity.preview)
    }

    @Test
    fun `items are unpinned by default`() {
        assertEquals(false, ClipboardEntity(content = "hello").isPinned)
    }

    @Test
    fun `categories are distinct constants`() {
        val categories = listOf(
            ClipboardEntity.CATEGORY_TEXT,
            ClipboardEntity.CATEGORY_LINK,
            ClipboardEntity.CATEGORY_EMAIL,
            ClipboardEntity.CATEGORY_PHONE,
            ClipboardEntity.CATEGORY_NUMBER,
            ClipboardEntity.CATEGORY_CODE,
            ClipboardEntity.CATEGORY_ADDRESS
        )
        assertEquals(categories.size, categories.toSet().size)
    }
}
