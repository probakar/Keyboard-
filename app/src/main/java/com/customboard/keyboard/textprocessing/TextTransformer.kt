package com.customboard.keyboard.textprocessing

import java.util.Locale

/** Case conversions and other quick text operations offered by the text tools panel. */
object TextTransformer {

    enum class Transform(val id: String) {
        UPPERCASE("uppercase"),
        LOWERCASE("lowercase"),
        TITLE_CASE("title_case"),
        SENTENCE_CASE("sentence_case"),
        TOGGLE_CASE("toggle_case"),
        CAMEL_CASE("camel_case"),
        SNAKE_CASE("snake_case"),
        KEBAB_CASE("kebab_case"),
        REVERSE("reverse"),
        TRIM_SPACES("trim_spaces"),
        REMOVE_LINE_BREAKS("remove_line_breaks"),
        SORT_LINES("sort_lines"),
        REMOVE_DUPLICATE_LINES("remove_duplicate_lines"),
        ADD_QUOTES("add_quotes"),
        URL_ENCODE("url_encode")
    }

    fun apply(text: String, transform: Transform, locale: Locale = Locale.getDefault()): String =
        when (transform) {
            Transform.UPPERCASE -> text.uppercase(locale)
            Transform.LOWERCASE -> text.lowercase(locale)
            Transform.TITLE_CASE -> titleCase(text, locale)
            Transform.SENTENCE_CASE -> sentenceCase(text, locale)
            Transform.TOGGLE_CASE -> text.map {
                if (it.isUpperCase()) it.lowercaseChar() else it.uppercaseChar()
            }.joinToString("")
            Transform.CAMEL_CASE -> camelCase(text)
            Transform.SNAKE_CASE -> words(text).joinToString("_") { it.lowercase(locale) }
            Transform.KEBAB_CASE -> words(text).joinToString("-") { it.lowercase(locale) }
            Transform.REVERSE -> text.reversed()
            Transform.TRIM_SPACES -> text.trim().replace(Regex("[ \\t]+"), " ")
            Transform.REMOVE_LINE_BREAKS -> text.replace(Regex("\\s*\\n+\\s*"), " ").trim()
            Transform.SORT_LINES -> text.lines().filter { it.isNotBlank() }.sorted().joinToString("\n")
            Transform.REMOVE_DUPLICATE_LINES -> text.lines().distinct().joinToString("\n")
            Transform.ADD_QUOTES -> "\u201C${text.trim()}\u201D"
            Transform.URL_ENCODE -> runCatching {
                java.net.URLEncoder.encode(text, "UTF-8")
            }.getOrDefault(text)
        }

    fun titleCase(text: String, locale: Locale = Locale.getDefault()): String =
        text.split(" ").joinToString(" ") { word ->
            if (word.isEmpty()) word
            else word[0].uppercase(locale) + word.substring(1).lowercase(locale)
        }

    fun sentenceCase(text: String, locale: Locale = Locale.getDefault()): String {
        val builder = StringBuilder(text.lowercase(locale))
        var capitalise = true
        for (index in builder.indices) {
            val char = builder[index]
            if (capitalise && char.isLetter()) {
                builder[index] = char.uppercaseChar()
                capitalise = false
            }
            if (char == '.' || char == '!' || char == '?' || char == '\n') capitalise = true
        }
        return builder.toString()
    }

    private fun camelCase(text: String): String {
        val parts = words(text)
        if (parts.isEmpty()) return text
        return parts.first().lowercase() + parts.drop(1).joinToString("") { part ->
            part.replaceFirstChar { it.uppercase() }
        }
    }

    private fun words(text: String): List<String> =
        text.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
}
