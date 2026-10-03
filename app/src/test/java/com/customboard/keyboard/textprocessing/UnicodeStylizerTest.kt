package com.customboard.keyboard.textprocessing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnicodeStylizerTest {

    @Test
    fun `every style changes plain ascii`() {
        UnicodeStylizer.Style.values().forEach { style ->
            val styled = UnicodeStylizer.apply("abc", style)
            assertNotEquals("style ${style.id} did nothing", "abc", styled)
        }
    }

    @Test
    fun `styles keep spaces and punctuation`() {
        val styled = UnicodeStylizer.apply("a b", UnicodeStylizer.Style.values().first())
        assertTrue(styled.contains(" "))
    }

    @Test
    fun `allStyles returns one entry per style`() {
        assertEquals(UnicodeStylizer.Style.values().size, UnicodeStylizer.allStyles("hi").size)
    }

    @Test
    fun `style ids are unique`() {
        val ids = UnicodeStylizer.Style.values().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
