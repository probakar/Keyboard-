package com.customboard.keyboard.layouts

import org.junit.Assert.assertTrue
import org.junit.Test

class EditorSpecificLayoutsTest {

    @Test
    fun `email layout surfaces at sign and com shortcut`() {
        val keys = EditorSpecificLayouts.email(QwertyLayout.create()).rows.last().keys
        assertTrue(keys.any { it.outputText == "@" })
        assertTrue(keys.any { it.outputText == ".com" })
    }

    @Test
    fun `url layout surfaces slash and web prefixes`() {
        val keys = EditorSpecificLayouts.url(QwertyLayout.create()).rows.last().keys
        assertTrue(keys.any { it.outputText == "/" })
        assertTrue(keys.any { it.outputText == "www." })
        assertTrue(keys.any { it.outputText == ".com" })
    }
}
