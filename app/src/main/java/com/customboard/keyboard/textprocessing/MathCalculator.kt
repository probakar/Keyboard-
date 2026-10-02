package com.customboard.keyboard.textprocessing

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Inline calculator: when the user types "12*7+3" the result appears in the suggestion strip,
 * exactly like Samsung Keyboard and Gboard do.
 */
object MathCalculator {

    private val EXPRESSION = Regex("^[-+*/^().,%\\d\\s a-z]+$")

    /** Returns the formatted result or null when [input] is not an expression. */
    fun evaluate(input: String): String? {
        val text = input.trim().removeSuffix("=").trim()
        if (text.length < 3 || text.length > 120) return null
        if (!text.any { it.isDigit() }) return null
        if (!text.any { it in "+-*/^%" }) return null
        if (!EXPRESSION.matches(text.lowercase())) return null
        return runCatching {
            val value = Parser(text.lowercase().replace(",", "")).parse()
            if (value.isNaN() || value.isInfinite()) null else format(value)
        }.getOrNull()
    }

    fun format(value: Double): String {
        val rounded = (value * 1_000_000.0).roundToLong() / 1_000_000.0
        return if (abs(rounded - rounded.toLong()) < 1e-9) rounded.toLong().toString()
        else rounded.toString()
    }

    /** Recursive descent parser supporting + - * / % ^ parentheses and a few functions. */
    private class Parser(private val source: String) {
        private var position = 0

        fun parse(): Double {
            val value = parseExpression()
            skipSpaces()
            if (position < source.length) throw IllegalArgumentException("unexpected")
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                when {
                    consume('+') -> value += parseTerm()
                    consume('-') -> value -= parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseFactor()
            while (true) {
                skipSpaces()
                when {
                    consume('*') -> value *= parseFactor()
                    consume('/') -> value /= parseFactor()
                    consume('%') -> value %= parseFactor()
                    else -> return value
                }
            }
        }

        private fun parseFactor(): Double {
            skipSpaces()
            if (consume('-')) return -parseFactor()
            if (consume('+')) return parseFactor()
            var value = parseAtom()
            skipSpaces()
            if (consume('^')) value = value.pow(parseFactor())
            return value
        }

        private fun parseAtom(): Double {
            skipSpaces()
            if (consume('(')) {
                val value = parseExpression()
                skipSpaces()
                if (!consume(')')) throw IllegalArgumentException("missing )")
                return value
            }
            val start = position
            while (position < source.length && (source[position].isDigit() || source[position] == '.')) {
                position++
            }
            if (position > start) return source.substring(start, position).toDouble()

            val nameStart = position
            while (position < source.length && source[position].isLetter()) position++
            if (position > nameStart) {
                val name = source.substring(nameStart, position)
                if (name == "pi") return Math.PI
                if (name == "e") return Math.E
                val argument = parseAtom()
                return when (name) {
                    "sqrt" -> sqrt(argument)
                    "sin" -> sin(Math.toRadians(argument))
                    "cos" -> cos(Math.toRadians(argument))
                    "tan" -> tan(Math.toRadians(argument))
                    "log" -> log10(argument)
                    "ln" -> ln(argument)
                    "abs" -> abs(argument)
                    else -> throw IllegalArgumentException("unknown $name")
                }
            }
            throw IllegalArgumentException("unexpected char")
        }

        private fun consume(char: Char): Boolean {
            skipSpaces()
            if (position < source.length && source[position] == char) {
                position++
                return true
            }
            return false
        }

        private fun skipSpaces() {
            while (position < source.length && source[position] == ' ') position++
        }
    }
}
