package com.customboard.keyboard.ai

/**
 * Prompt templates. They are intentionally strict: the model must answer with the rewritten
 * text only, so the result can be dropped straight into the text field.
 */
object AiPromptLibrary {

    const val SYSTEM_EDITOR =
        "You are the writing assistant built into a mobile keyboard. " +
            "Reply with the requested text only - no preamble, no explanation, no quotes, " +
            "no markdown fences. Keep the author's original language unless asked otherwise. " +
            "Preserve names, numbers, links and formatting. Never add commentary."

    const val SYSTEM_ASSISTANT =
        "You are a concise assistant inside a mobile keyboard. Answer in at most 60 words, " +
            "in plain text, ready to be pasted into a chat message."

    fun proofread(text: String) =
        "Correct the spelling, grammar and punctuation of the text below. " +
            "Keep the wording and tone as close to the original as possible.\n\n$text"

    fun rewrite(text: String) =
        "Rewrite the text below so it reads clearly and naturally, keeping the same meaning " +
            "and roughly the same length.\n\n$text"

    fun tone(text: String, tone: String) =
        "Rewrite the text below in a $tone tone. Keep the meaning and the language.\n\n$text"

    fun summarize(text: String) =
        "Summarise the text below in one or two short sentences.\n\n$text"

    fun expand(text: String) =
        "Expand the text below into a fuller version with more detail, " +
            "staying on topic and keeping the tone.\n\n$text"

    fun shorten(text: String) =
        "Shorten the text below to the essentials, keeping the tone. " +
            "Aim for about half the length.\n\n$text"

    fun bullets(text: String) =
        "Turn the text below into a short bullet list. Use the character • for every bullet " +
            "and put each bullet on its own line.\n\n$text"

    fun translate(text: String, targetLanguage: String) =
        "Translate the text below into $targetLanguage. Return only the translation.\n\n$text"

    fun emojify(text: String) =
        "Rewrite the text below adding a few fitting emoji. Do not change the words " +
            "themselves, only insert emoji where they help.\n\n$text"

    fun explain(text: String) =
        "Explain the following in one or two simple sentences:\n\n$text"

    fun continueWriting(text: String) =
        "Continue the text below with one or two sentences that match the style and tone. " +
            "Return only the continuation, starting with the next word.\n\n$text"

    fun reply(conversation: String, tone: String) =
        "Write a $tone reply to the message below. Keep it short and natural, " +
            "as a chat message.\n\n$conversation"

    fun compose(instruction: String) =
        "Write the message described below. Keep it ready to send, with no placeholders.\n\n$instruction"

    fun hashtags(text: String) =
        "Suggest 8 relevant hashtags for the text below. Return them on one line, " +
            "separated by spaces, each starting with #.\n\n$text"

    fun ask(question: String) = question

    fun custom(instruction: String, text: String) =
        if (text.isBlank()) instruction else "$instruction\n\n$text"

    fun smartReplies(conversation: String) =
        "Suggest three very short replies (max 6 words each) to the last message in this " +
            "conversation. Return them as three lines, nothing else.\n\n$conversation"
}
