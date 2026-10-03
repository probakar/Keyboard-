package com.customboard.keyboard.ai

import android.content.Context
import com.customboard.keyboard.R
import com.customboard.keyboard.autocorrect.GrammarChecker
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants

/**
 * Single entry point for every AI feature in the keyboard.
 *
 * On-device work (ML Kit smart reply, language id, offline translation) is preferred whenever
 * it can answer the request; Gemini handles the generative actions. Results are cached so
 * repeating an action is free.
 */
class AiWritingAssistant(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val gemini = GeminiClient(context)
    private val smartReply = SmartReplyManager(context)
    private val detector = LanguageDetector()
    private val translator = OnDeviceTranslator()

    val hasApiKey: Boolean get() = gemini.hasApiKey()

    val isEnabled: Boolean get() = prefs.aiEnabled

    /** Runs [action] over [text] and returns the text to insert. */
    suspend fun run(
        action: AiAction,
        text: String,
        extra: String = "",
        contextText: String = ""
    ): AiResult {
        if (!prefs.aiEnabled) {
            return AiResult.Error(context.getString(R.string.ai_error_disabled))
        }
        val input = text.trim().take(Constants.AI_MAX_INPUT_CHARS)

        AiResultCache.get(action.id, input, extra)?.let { return AiResult.Success(it) }

        val result = when (action) {
            AiAction.PROOFREAD -> proofread(input)
            AiAction.REWRITE -> ask(AiPromptLibrary.rewrite(input))
            AiAction.TONE -> ask(AiPromptLibrary.tone(input, extra.ifBlank { prefs.aiTone }))
            AiAction.SUMMARIZE -> ask(AiPromptLibrary.summarize(input))
            AiAction.EXPAND -> ask(AiPromptLibrary.expand(input), temperature = 0.7f)
            AiAction.SHORTEN -> ask(AiPromptLibrary.shorten(input))
            AiAction.BULLETS -> ask(AiPromptLibrary.bullets(input))
            AiAction.TRANSLATE -> translate(input, extra.ifBlank { prefs.aiTranslateTarget })
            AiAction.REPLY -> reply(input.ifBlank { contextText }, extra)
            AiAction.CONTINUE -> ask(AiPromptLibrary.continueWriting(input), temperature = 0.75f)
            AiAction.EMOJIFY -> ask(AiPromptLibrary.emojify(input), temperature = 0.8f)
            AiAction.EXPLAIN -> ask(AiPromptLibrary.explain(input), system = AiPromptLibrary.SYSTEM_ASSISTANT)
            AiAction.ASK -> ask(AiPromptLibrary.ask(input), system = AiPromptLibrary.SYSTEM_ASSISTANT)
            AiAction.COMPOSE -> ask(AiPromptLibrary.compose(input), temperature = 0.8f)
            AiAction.HASHTAGS -> ask(AiPromptLibrary.hashtags(input), temperature = 0.8f)
            AiAction.CUSTOM -> ask(AiPromptLibrary.custom(extra, input))
        }

        if (result is AiResult.Success) {
            AiResultCache.put(action.id, input, result.text, extra)
        }
        return result
    }

    private suspend fun ask(
        prompt: String,
        system: String = AiPromptLibrary.SYSTEM_EDITOR,
        temperature: Float = prefs.aiTemperature
    ): AiResult = gemini.generate(prompt, system, temperature).let { result ->
        if (result is AiResult.Success) AiResult.Success(clean(result.text)) else result
    }

    /**
     * Grammar fixes run locally first; the cloud is only used when the offline rules cannot
     * find anything, which keeps the common case instant and private.
     */
    private suspend fun proofread(text: String): AiResult {
        val local = GrammarChecker.autoFix(text)
        val hasLocalFix = local != text
        if (hasLocalFix && !prefs.aiProofread) return AiResult.Success(local)
        if (!gemini.hasApiKey()) {
            return if (hasLocalFix) AiResult.Success(local)
            else AiResult.Error(context.getString(R.string.ai_error_no_key), needsKey = true)
        }
        val remote = ask(AiPromptLibrary.proofread(text), temperature = 0.2f)
        return if (remote is AiResult.Error && hasLocalFix) AiResult.Success(local) else remote
    }

    private suspend fun translate(text: String, target: String): AiResult {
        if (text.isBlank()) return AiResult.Error(context.getString(R.string.ai_error_empty_input))
        val source = detector.detect(text)
        if (prefs.aiOfflineOnly && OnDeviceTranslator.supports(target)) {
            when (val outcome = translator.translate(text, target, source)) {
                is OnDeviceTranslator.Outcome.Success -> return AiResult.Success(outcome.text)
                else -> Unit // fall through to the cloud
            }
        }
        val languageName = languageName(target)
        return ask(AiPromptLibrary.translate(text, languageName), temperature = 0.2f)
    }

    private suspend fun reply(conversation: String, tone: String): AiResult {
        if (conversation.isBlank()) {
            return AiResult.Error(context.getString(R.string.ai_error_no_conversation))
        }
        val offline = smartReply.suggestFor(conversation)
        if (offline.isNotEmpty() && tone.isBlank()) {
            return AiResult.Success(offline.joinToString("\n"))
        }
        return ask(
            AiPromptLibrary.reply(conversation, tone.ifBlank { prefs.aiTone }),
            temperature = 0.8f
        )
    }

    /** Three short replies for the suggestion strip (on-device, no key needed). */
    suspend fun quickReplies(lastMessage: String): List<String> =
        if (!prefs.aiSmartReply) emptyList() else smartReply.suggestFor(lastMessage)

    suspend fun detectLanguage(text: String): String? = detector.detect(text)

    suspend fun translateOffline(text: String, target: String): String? =
        (translator.translate(text, target, detector.detect(text)) as? OnDeviceTranslator.Outcome.Success)?.text

    /** Splits a multi-line AI answer into selectable options. */
    fun asOptions(text: String): List<String> = text.lines()
        .map { it.trim().removePrefix("-").removePrefix("*").trim() }
        .filter { it.isNotEmpty() }

    private fun clean(text: String): String = text
        .trim()
        .removeSurrounding("\"")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    fun languageName(tag: String): String {
        val codes = context.resources.getStringArray(R.array.translate_values)
        val names = context.resources.getStringArray(R.array.translate_entries)
        val index = codes.indexOf(tag)
        return if (index >= 0 && index < names.size) names[index] else tag
    }

    fun release() {
        smartReply.release()
        detector.release()
        translator.release()
    }
}
