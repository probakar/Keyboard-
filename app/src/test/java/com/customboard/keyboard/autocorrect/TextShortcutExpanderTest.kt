package com.customboard.keyboard.autocorrect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextShortcutExpanderTest {

    @Test
    fun `expands matching shortcut case insensitively`() {
        assertEquals("By the way", TextShortcutExpander.expand("btw", "btw=By the way"))
    }

    @Test
    fun `mirrors initial capitalization and all caps`() {
        val definitions = "omw=on my way"
        assertEquals("On my way", TextShortcutExpander.expand("Omw", definitions))
        assertEquals("ON MY WAY", TextShortcutExpander.expand("OMW", definitions))
    }

    @Test
    fun `ignores malformed or empty definitions`() {
        assertNull(TextShortcutExpander.expand("addr", "bad line\n=missing key\nempty=   "))
    }

    @Test
    fun `keeps equals signs inside expansion`() {
        assertEquals("a=b", TextShortcutExpander.expand("equ", "equ=a=b"))
    }
}
