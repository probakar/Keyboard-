package com.customboard.keyboard.voice

/**
 * Understands the spoken editing commands ("new line", "comma", "delete that", "send") so
 * dictation feels like Samsung's and Google's voice typing.
 */
object VoiceCommandProcessor {

    sealed class Command {
        data class Insert(val text: String) : Command()
        object NewLine : Command()
        object DeleteWord : Command()
        object DeleteAll : Command()
        object Send : Command()
        object Stop : Command()
        object Undo : Command()
        object SelectAll : Command()
    }

    private val PUNCTUATION = mapOf(
        "comma" to ",", "period" to ".", "full stop" to ".", "question mark" to "?",
        "exclamation mark" to "!", "exclamation point" to "!", "colon" to ":",
        "semicolon" to ";", "dash" to "-", "hyphen" to "-", "underscore" to "_",
        "open parenthesis" to "(", "close parenthesis" to ")", "quote" to "\"",
        "apostrophe" to "'", "at sign" to "@", "hashtag" to "#", "dollar sign" to "$",
        "percent sign" to "%", "ampersand" to "&", "asterisk" to "*", "plus sign" to "+",
        "equals sign" to "=", "slash" to "/", "backslash" to "\\", "ellipsis" to "...",
        "smiley face" to "\uD83D\uDE42", "heart" to "\u2764\uFE0F", "thumbs up" to "\uD83D\uDC4D"
    )

    private val COMMANDS = mapOf(
        "new line" to Command.NewLine,
        "next line" to Command.NewLine,
        "new paragraph" to Command.NewLine,
        "enter" to Command.NewLine,
        "delete" to Command.DeleteWord,
        "delete that" to Command.DeleteWord,
        "backspace" to Command.DeleteWord,
        "scratch that" to Command.DeleteWord,
        "clear all" to Command.DeleteAll,
        "clear text" to Command.DeleteAll,
        "select all" to Command.SelectAll,
        "undo" to Command.Undo,
        "undo that" to Command.Undo,
        "send message" to Command.Send,
        "send it" to Command.Send,
        "stop listening" to Command.Stop,
        "stop dictation" to Command.Stop
    )

    /** Converts a recognised utterance into the commands/insertions it represents. */
    fun process(utterance: String, enabled: Boolean): List<Command> {
        val text = utterance.trim()
        if (text.isEmpty()) return emptyList()
        if (!enabled) return listOf(Command.Insert(text))

        val lower = text.lowercase()
        COMMANDS[lower]?.let { return listOf(it) }

        var result = text
        PUNCTUATION.forEach { (spoken, symbol) ->
            result = result.replace(Regex("(?i)\\b$spoken\\b"), symbol)
        }
        result = result
            .replace(Regex("\\s+([,.;:!?])"), "$1")
            .replace(Regex("(?i)\\bnew line\\b"), "\n")
            .trim()

        return if (result.isEmpty()) emptyList() else listOf(Command.Insert(result))
    }

    fun isCommand(utterance: String): Boolean = COMMANDS.containsKey(utterance.trim().lowercase())
}
