package com.customboard.keyboard.ai

import android.content.Context
import com.customboard.keyboard.BuildConfig
import com.customboard.keyboard.R
import com.customboard.keyboard.privacy.SecureStorage
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.KeyboardUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin, dependency free client for the Google AI (Gemini) REST API.
 *
 * The key is supplied by the user and stored encrypted on the device; requests only contain the
 * text the user explicitly sends to an AI action.
 */
class GeminiClient(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val secureStorage = SecureStorage.getInstance(context)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(Constants.AI_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun apiKey(): String {
        val stored = secureStorage.geminiApiKey
        return if (stored.isNotBlank()) stored else BuildConfig.DEFAULT_GEMINI_API_KEY
    }

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    /**
     * Sends [prompt] to Gemini and returns the plain text answer.
     *
     * @param systemInstruction behaviour given to the model
     * @param temperature 0..1, defaults to the value configured in settings
     */
    suspend fun generate(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = prefs.aiTemperature,
        model: String = prefs.aiModel,
        maxOutputTokens: Int = 1024
    ): AiResult = withContext(Dispatchers.IO) {
        val key = apiKey()
        if (key.isBlank()) {
            return@withContext AiResult.Error(context.getString(R.string.ai_error_no_key), needsKey = true)
        }
        if (!KeyboardUtils.hasInternet(context)) {
            return@withContext AiResult.Error(context.getString(R.string.ai_error_offline))
        }
        val trimmed = prompt.take(Constants.AI_MAX_INPUT_CHARS)
        if (trimmed.isBlank()) {
            return@withContext AiResult.Error(context.getString(R.string.ai_error_empty_input))
        }

        val payload = buildPayload(trimmed, systemInstruction, temperature, maxOutputTokens)
        val url = "${Constants.GEMINI_BASE_URL}$model:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", key)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiResult.Error(describeError(response.code, body), needsKey = response.code == 400 || response.code == 403)
                }
                val text = parseText(body)
                if (text.isNullOrBlank()) {
                    val blocked = parseBlockReason(body)
                    return@withContext AiResult.Error(
                        blocked ?: context.getString(R.string.ai_error_empty_response)
                    )
                }
                AiResult.Success(text.trim())
            }
        } catch (e: IOException) {
            AiResult.Error(context.getString(R.string.ai_error_network))
        } catch (e: Exception) {
            AiResult.Error(e.localizedMessage ?: context.getString(R.string.ai_error_generic))
        }
    }

    private fun buildPayload(
        prompt: String,
        systemInstruction: String?,
        temperature: Float,
        maxOutputTokens: Int
    ): JSONObject {
        val part = JSONObject().put("text", prompt)
        val content = JSONObject()
            .put("role", "user")
            .put("parts", JSONArray().put(part))

        val generationConfig = JSONObject()
            .put("temperature", temperature.coerceIn(0f, 1f).toDouble())
            .put("topK", 40)
            .put("topP", 0.95)
            .put("maxOutputTokens", maxOutputTokens)
            .put("candidateCount", 1)

        val safety = JSONArray()
        listOf(
            "HARM_CATEGORY_HARASSMENT",
            "HARM_CATEGORY_HATE_SPEECH",
            "HARM_CATEGORY_SEXUALLY_EXPLICIT",
            "HARM_CATEGORY_DANGEROUS_CONTENT"
        ).forEach { category ->
            safety.put(
                JSONObject()
                    .put("category", category)
                    .put("threshold", "BLOCK_ONLY_HIGH")
            )
        }

        val payload = JSONObject()
            .put("contents", JSONArray().put(content))
            .put("generationConfig", generationConfig)
            .put("safetySettings", safety)

        if (!systemInstruction.isNullOrBlank()) {
            payload.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
            )
        }
        return payload
    }

    private fun parseText(body: String): String? = runCatching {
        val candidates = JSONObject(body).optJSONArray("candidates") ?: return@runCatching null
        if (candidates.length() == 0) return@runCatching null
        val parts = candidates.getJSONObject(0)
            .optJSONObject("content")
            ?.optJSONArray("parts") ?: return@runCatching null
        val builder = StringBuilder()
        for (i in 0 until parts.length()) {
            builder.append(parts.getJSONObject(i).optString("text"))
        }
        builder.toString()
    }.getOrNull()

    private fun parseBlockReason(body: String): String? = runCatching {
        val feedback = JSONObject(body).optJSONObject("promptFeedback") ?: return@runCatching null
        val reason = feedback.optString("blockReason")
        if (reason.isNullOrBlank()) null else context.getString(R.string.ai_error_blocked)
    }.getOrNull()

    private fun describeError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when (code) {
            400 -> message ?: context.getString(R.string.ai_error_bad_request)
            401, 403 -> context.getString(R.string.ai_error_invalid_key)
            429 -> context.getString(R.string.ai_error_rate_limited)
            in 500..599 -> context.getString(R.string.ai_error_server)
            else -> message ?: context.getString(R.string.ai_error_generic)
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

/** Result of an AI call. */
sealed class AiResult {
    data class Success(val text: String) : AiResult()
    data class Error(val message: String, val needsKey: Boolean = false) : AiResult()

    val textOrNull: String? get() = (this as? Success)?.text
}
