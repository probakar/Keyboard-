package com.customboard.keyboard.textprocessing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class TextTransformerTest {

    private fun transform(text: String, transform: TextTransformer.Transform): String =
        TextTransformer.apply(text, transform, Locale.ENGLISH)

    @Test
    fun `uppercase and lowercase round trip`() {
        assertEquals("HELLO WORLD", transform("Hello World", TextTransformer.Transform.UPPERCASE))
        assertEquals("hello world", transform("Hello World", TextTransformer.Transform.LOWERCASE))
    }

    @Test
    fun `title case capitalises every word`() {
        assertEquals("Hello Big World", transform("hello big world", TextTransformer.Transform.TITLE_CASE))
    }

    @Test
    fun `sentence case only capitalises sentence starts`() {
        assertEquals(
            "Hello there. How are you?",
            transform("hello there. how are you?", TextTransformer.Transform.SENTENCE_CASE)
        )
    }

    @Test
    fun `toggle case flips every letter`() {
        assertEquals("hELLO", transform("Hello", TextTransformer.Transform.TOGGLE_CASE))
    }

    @Test
    fun `programmer cases strip separators`() {
        assertEquals("helloBigWorld", transform("hello big world", TextTransformer.Transform.CAMEL_CASE))
        assertEquals("hello_big_world", transform("hello big world", TextTransformer.Transform.SNAKE_CASE))
        assertEquals("hello-big-world", transform("hello big world", TextTransformer.Transform.KEBAB_CASE))
    }

    @Test
    fun `reverse mirrors the string`() {
        assertEquals("cba", transform("abc", TextTransformer.Transform.REVERSE))
    }

    @Test
    fun `trim spaces collapses runs of whitespace`() {
        assertEquals("a b c", transform("  a   b    c  ", TextTransformer.Transform.TRIM_SPACES))
    }

    @Test
    fun `line transforms operate per line`() {
        assertEquals("a b", transform("a\nb", TextTransformer.Transform.REMOVE_LINE_BREAKS))
        assertEquals("a\nb\nc", transform("c\nb\na", TextTransformer.Transform.SORT_LINES))
        assertEquals("a\nb", transform("a\nb\na", TextTransformer.Transform.REMOVE_DUPLICATE_LINES))
    }

    @Test
    fun `quotes use typographic characters`() {
        assertEquals("\u201Chi\u201D", transform("hi", TextTransformer.Transform.ADD_QUOTES))
    }

    @Test
    fun `url encoding escapes spaces`() {
        assertEquals("a+b", transform("a b", TextTransformer.Transform.URL_ENCODE))
    }

    @Test
    fun `no transform throws on empty input`() {
        TextTransformer.Transform.values().forEach { value ->
            transform("", value)
        }
    }
}
