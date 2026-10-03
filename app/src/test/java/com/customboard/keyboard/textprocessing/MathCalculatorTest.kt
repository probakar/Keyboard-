package com.customboard.keyboard.textprocessing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MathCalculatorTest {

    @Test
    fun `evaluates the four basic operators`() {
        assertEquals("4", MathCalculator.evaluate("2+2"))
        assertEquals("6", MathCalculator.evaluate("8-2"))
        assertEquals("12", MathCalculator.evaluate("3*4"))
        assertEquals("2.5", MathCalculator.evaluate("5/2"))
    }

    @Test
    fun `respects operator precedence and parentheses`() {
        assertEquals("14", MathCalculator.evaluate("2+3*4"))
        assertEquals("20", MathCalculator.evaluate("(2+3)*4"))
    }

    @Test
    fun `supports powers and the modulo operator`() {
        assertEquals("8", MathCalculator.evaluate("2^3"))
        assertEquals("1", MathCalculator.evaluate("10%3"))
    }

    @Test
    fun `accepts a trailing equals sign`() {
        assertEquals("7", MathCalculator.evaluate("3+4="))
    }

    @Test
    fun `rejects plain prose and incomplete input`() {
        assertNull(MathCalculator.evaluate("hello there"))
        assertNull(MathCalculator.evaluate("12"))
        assertNull(MathCalculator.evaluate(""))
        assertNull(MathCalculator.evaluate("2+"))
    }

    @Test
    fun `rejects division by zero`() {
        assertNull(MathCalculator.evaluate("1/0"))
    }
}
