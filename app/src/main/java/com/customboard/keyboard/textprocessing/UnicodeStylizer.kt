package com.customboard.keyboard.textprocessing

/**
 * Turns plain text into the Unicode "fonts" people use on social media
 * (𝗯𝗼𝗹𝗱, 𝘪𝘵𝘢𝘭𝘪𝘤, 𝓼𝓬𝓻𝓲𝓹𝓽, Ⓒⓘⓡⓒⓛⓔⓓ, ｆｕｌｌｗｉｄｔｈ, s̶t̶r̶i̶k̶e̶ ...).
 *
 * Everything is pure code point arithmetic, so no fonts or assets are needed.
 */
object UnicodeStylizer {

    enum class Style(val id: String, val label: String) {
        NORMAL("normal", "Normal"),
        BOLD("bold", "\uD835\uDDD5\uD835\uDDFC\uD835\uDE05\uD835\uDDF1"),
        ITALIC("italic", "\uD835\uDE10\uD835\uDE35\uD835\uDE22\uD835\uDE2D\uD835\uDE2A\uD835\uDE24"),
        BOLD_ITALIC("bold_italic", "\uD835\uDE23\uD835\uDE2A"),
        SCRIPT("script", "\uD835\uDCE2\uD835\uDCEC\uD835\uDCFB\uD835\uDCF2\uD835\uDCFD"),
        FRAKTUR("fraktur", "\uD835\uDD89\uD835\uDD94\uD835\uDD91"),
        DOUBLE_STRUCK("double", "\uD835\uDD3B\uD835\uDD60\uD835\uDD66\uD835\uDD53"),
        MONOSPACE("mono", "\uD835\uDE7C\uD835\uDE98\uD835\uDE97\uD835\uDE98"),
        CIRCLED("circled", "Ⓒⓘⓡ"),
        SQUARED("squared", "\uD83C\uDDE2\uD83C\uDDE6"),
        FULLWIDTH("fullwidth", "Ｆｕｌｌ"),
        SMALL_CAPS("small_caps", "sᴍᴀʟʟ"),
        UPSIDE_DOWN("upside_down", "uʍop"),
        STRIKETHROUGH("strike", "s̶t̶r̶i̶k̶e̶"),
        UNDERLINE("underline", "u̲n̲d̲e̲r̲"),
        SPACED("spaced", "s p a c e d"),
        WAVY("wavy", "｡･:*ｗａｖｙ*:･｡");
    }

    fun apply(text: String, style: Style): String = when (style) {
        Style.NORMAL -> text
        Style.BOLD -> mapRanges(text, 0x1D5D4, 0x1D5EE, 0x1D7EC)
        Style.ITALIC -> mapRanges(text, 0x1D608, 0x1D622, null)
        Style.BOLD_ITALIC -> mapRanges(text, 0x1D63C, 0x1D656, null)
        Style.SCRIPT -> mapRanges(text, 0x1D4D0, 0x1D4EA, null)
        Style.FRAKTUR -> mapRanges(text, 0x1D56C, 0x1D586, null)
        Style.DOUBLE_STRUCK -> mapRanges(text, 0x1D538, 0x1D552, 0x1D7D8)
        Style.MONOSPACE -> mapRanges(text, 0x1D670, 0x1D68A, 0x1D7F6)
        Style.CIRCLED -> circled(text)
        Style.SQUARED -> squared(text)
        Style.FULLWIDTH -> fullwidth(text)
        Style.SMALL_CAPS -> lookup(text, SMALL_CAPS)
        Style.UPSIDE_DOWN -> lookup(text.reversed(), UPSIDE_DOWN)
        Style.STRIKETHROUGH -> combine(text, '\u0336')
        Style.UNDERLINE -> combine(text, '\u0332')
        Style.SPACED -> text.toCharArray().joinToString(" ")
        Style.WAVY -> "｡･:*:･ﾟ★," + fullwidth(text) + ",｡･:*:･ﾟ☆"
    }

    /** All styles applied to [text] - used by the text tools preview list. */
    fun allStyles(text: String): List<Pair<Style, String>> =
        Style.entries.filter { it != Style.NORMAL }.map { it to apply(text, it) }

    private fun mapRanges(text: String, upperBase: Int, lowerBase: Int, digitBase: Int?): String {
        val builder = StringBuilder(text.length * 2)
        for (char in text) {
            val codePoint = when {
                char in 'A'..'Z' -> upperBase + (char - 'A')
                char in 'a'..'z' -> lowerBase + (char - 'a')
                digitBase != null && char in '0'..'9' -> digitBase + (char - '0')
                else -> char.code
            }
            if (codePoint > 0xFFFF) builder.appendCodePoint(codePoint) else builder.append(char)
        }
        return builder.toString()
    }

    private fun circled(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            val codePoint = when {
                char in 'A'..'Z' -> 0x24B6 + (char - 'A')
                char in 'a'..'z' -> 0x24D0 + (char - 'a')
                char in '1'..'9' -> 0x2460 + (char - '1')
                char == '0' -> 0x24EA
                else -> char.code
            }
            builder.appendCodePoint(codePoint)
        }
        return builder.toString()
    }

    private fun squared(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            when {
                char in 'A'..'Z' -> builder.appendCodePoint(0x1F130 + (char - 'A'))
                char in 'a'..'z' -> builder.appendCodePoint(0x1F130 + (char - 'a'))
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun fullwidth(text: String): String {
        val builder = StringBuilder()
        for (char in text) {
            when {
                char == ' ' -> builder.append('\u3000')
                char.code in 0x21..0x7E -> builder.appendCodePoint(char.code - 0x21 + 0xFF01)
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    private fun lookup(text: String, table: Map<Char, Char>): String {
        val builder = StringBuilder(text.length)
        for (char in text) builder.append(table[char] ?: table[char.lowercaseChar()] ?: char)
        return builder.toString()
    }

    private fun combine(text: String, mark: Char): String {
        val builder = StringBuilder(text.length * 2)
        for (char in text) {
            builder.append(char)
            if (!char.isWhitespace()) builder.append(mark)
        }
        return builder.toString()
    }

    private val SMALL_CAPS: Map<Char, Char> = buildMap {
        val source = "abcdefghijklmnopqrstuvwxyz"
        val target = "ᴀʙᴄᴅᴇfɢʜɪᴊᴋʟᴍɴᴏᴘqʀsᴛᴜᴠᴡxʏᴢ"
        source.forEachIndexed { index, char -> put(char, target[index]) }
    }

    private val UPSIDE_DOWN: Map<Char, Char> = buildMap {
        val source = "abcdefghijklmnopqrstuvwxyz0123456789,.?!'\"()[]{}<>&_"
        val target = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz0ƖᄅƐㄣϛ9ㄥ86'˙¿¡,„)(][}{><⅋‾"
        source.forEachIndexed { index, char -> if (index < target.length) put(char, target[index]) }
    }
}
