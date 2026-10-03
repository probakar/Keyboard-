package com.customboard.keyboard.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.customboard.keyboard.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Opens web searches and fetches opt-in inline answer cards for the keyboard search panel. */
class WebSearchManager(private val context: Context) {

    data class InlineResult(val title: String, val snippet: String, val url: String)

    enum class Provider(val id: String, val urlTemplate: String) {
        GOOGLE("google", "https://www.google.com/search?q=%s"),
        IMAGES("images", "https://www.google.com/search?tbm=isch&q=%s"),
        MAPS("maps", "https://www.google.com/maps/search/%s"),
        TRANSLATE("translate", "https://translate.google.com/?text=%s"),
        YOUTUBE("youtube", "https://www.youtube.com/results?search_query=%s"),
        DEFINITION("definition", "https://www.google.com/search?q=define+%s")
    }

    fun search(query: String, provider: Provider = Provider.GOOGLE) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            Toast.makeText(context, R.string.search_nothing_selected, Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrDefault(trimmed)
        open(provider.urlTemplate.format(encoded))
    }

    /**
     * Loads DuckDuckGo's public Instant Answer response only after the user explicitly asks for
     * quick results. This returns answer cards, not a full search-engine results page.
     */
    fun searchInline(
        query: String,
        onComplete: (List<InlineResult>, String?) -> Unit
    ): Call? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            mainHandler.post { onComplete(emptyList(), null) }
            return null
        }
        val encoded = runCatching { URLEncoder.encode(trimmed, "UTF-8") }.getOrDefault(trimmed)
        val request = Request.Builder()
            .url("https://api.duckduckgo.com/?q=$encoded&format=json&no_redirect=1&no_html=1&skip_disambig=1")
            .header("User-Agent", "CustomBoard-Android")
            .build()
        return inlineClient.newCall(request).also { call ->
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (!call.isCanceled) mainHandler.post { onComplete(emptyList(), error.message) }
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        if (!it.isSuccessful) {
                            emptyList<InlineResult>() to "HTTP ${it.code}"
                        } else {
                            runCatching {
                                val body = it.body?.string().orEmpty()
                                parseInlineResults(JSONObject(body), trimmed) to null
                            }.getOrElse { error -> emptyList<InlineResult>() to error.message }
                        }
                    }
                    if (!call.isCanceled) mainHandler.post { onComplete(result.first, result.second) }
                }
            })
        }
    }

    private fun parseInlineResults(document: JSONObject, query: String): List<InlineResult> {
        val results = LinkedHashMap<String, InlineResult>()
        val heading = document.optString("Heading").trim().ifBlank { query }
        val abstractText = document.optString("AbstractText").trim()
        val abstractUrl = document.optString("AbstractURL").trim()
        if (abstractText.isNotEmpty()) {
            results[abstractUrl.ifBlank { "answer:$heading" }] =
                InlineResult(heading, abstractText, abstractUrl)
        }

        val definition = document.optString("Definition").trim()
        val definitionUrl = document.optString("DefinitionURL").trim()
        if (definition.isNotEmpty()) {
            results.putIfAbsent(
                definitionUrl.ifBlank { "definition:$heading" },
                InlineResult(document.optString("DefinitionSource").ifBlank { "Definition" }, definition, definitionUrl)
            )
        }

        appendRelatedTopics(document.optJSONArray("RelatedTopics"), results)
        return results.values.take(MAX_INLINE_RESULTS)
    }

    private fun appendRelatedTopics(
        topics: JSONArray?,
        results: LinkedHashMap<String, InlineResult>
    ) {
        if (topics == null) return
        for (index in 0 until topics.length()) {
            val topic = topics.optJSONObject(index) ?: continue
            val nested = topic.optJSONArray("Topics")
            if (nested != null) {
                appendRelatedTopics(nested, results)
                continue
            }
            val text = topic.optString("Text").replace(HTML_TAGS, "").trim()
            val url = topic.optString("FirstURL").trim()
            if (text.isBlank() || !url.startsWith("https://")) continue
            val separator = text.indexOf(" - ")
            val title = if (separator > 0) text.substring(0, separator).trim() else text.take(72)
            val snippet = if (separator > 0) text.substring(separator + 3).trim() else text
            results.putIfAbsent(url, InlineResult(title, snippet, url))
            if (results.size >= MAX_INLINE_RESULTS * 2) return
        }
    }

    fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.search_no_browser, Toast.LENGTH_SHORT).show()
        }
    }

    /** Shares text with any app that accepts it. */
    fun share(text: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.action_share))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(chooser) }
    }

    companion object {
        private const val MAX_INLINE_RESULTS = 5
        private val HTML_TAGS = Regex("<[^>]*>")
        private val mainHandler = Handler(Looper.getMainLooper())
        private val inlineClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .build()
    }
}
