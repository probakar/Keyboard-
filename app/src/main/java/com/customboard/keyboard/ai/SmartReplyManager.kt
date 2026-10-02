package com.customboard.keyboard.ai

import android.content.Context
import android.util.Log
import com.customboard.keyboard.settings.PreferencesManager
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device smart replies (ML Kit). Runs completely offline, costs nothing and needs no key,
 * so it is the default source of reply suggestions; Gemini is used when the user wants a
 * longer or differently toned answer.
 */
class SmartReplyManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val client by lazy { SmartReply.getClient() }

    private val conversation = ArrayList<TextMessage>()

    /** Remembers an incoming message so replies can be generated for it. */
    fun addRemoteMessage(text: String, userId: String = "them") {
        if (text.isBlank()) return
        conversation += TextMessage.createForRemoteUser(text.take(400), System.currentTimeMillis(), userId)
        trim()
    }

    fun addLocalMessage(text: String) {
        if (text.isBlank()) return
        conversation += TextMessage.createForLocalUser(text.take(400), System.currentTimeMillis())
        trim()
    }

    fun clear() = conversation.clear()

    val hasConversation: Boolean get() = conversation.isNotEmpty()

    suspend fun suggest(): List<String> = withContext(Dispatchers.Default) {
        if (!prefs.aiSmartReply || conversation.isEmpty()) return@withContext emptyList()
        runCatching {
            val result = client.suggestReplies(conversation).await()
            if (result.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                result.suggestions.map { it.text }
            } else {
                emptyList()
            }
        }.onFailure { Log.w(TAG, "smart reply failed", it) }.getOrDefault(emptyList())
    }

    /** Builds a conversation out of a single message, for apps where only the draft is visible. */
    suspend fun suggestFor(message: String): List<String> {
        clear()
        addRemoteMessage(message)
        return suggest()
    }

    fun release() = runCatching { client.close() }

    private fun trim() {
        while (conversation.size > 10) conversation.removeAt(0)
    }

    companion object {
        private const val TAG = "SmartReply"
    }
}
