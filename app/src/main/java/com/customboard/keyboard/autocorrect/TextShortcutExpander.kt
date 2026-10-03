package com.customboard.keyboard.autocorrect

/** Expands user-defined `short=long text` entries when a word is committed. */
object TextShortcutExpander {

    fun expand(typed: String, definitions: String): String? {
        if (typed.isBlank() || definitions.isBlank()) return null
        val match = definitions.lineSequence()
            .mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                val shortcut = line.substring(0, separator).trim()
                val expansion = line.substring(separator + 1).trim()
                if (shortcut.isEmpty() || expansion.isEmpty()) null else shortcut to expansion
            }
            .firstOrNull { (shortcut, _) -> shortcut.equals(typed, ignoreCase = true) }
            ?: return null

        val expansion = match.second
        return when {
            typed.length > 1 && typed.all { it.isUpperCase() } -> expansion.uppercase()
            typed.firstOrNull()?.isUpperCase() == true -> expansion.replaceFirstChar { it.uppercase() }
            else -> expansion
        }
    }
}
