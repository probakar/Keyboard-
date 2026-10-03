package com.customboard.keyboard.emoji

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.customboard.keyboard.BuildConfig
import com.customboard.keyboard.privacy.SecureStorage
import com.customboard.keyboard.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * GIF search powered by Google's Tenor API.
 *
 * The user supplies their own Tenor key in settings (the free tier is enough); without a key
 * the GIF tab explains how to add one instead of failing silently.
 */
class GifSearchManager(private val context: Context) {

    data class Gif(val id: String, val previewUrl: String, val fullUrl: String, val description: String)

    private val secureStorage = SecureStorage.getInstance(context)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private val thumbnailCache = object : LruCache<String, Bitmap>(6 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun apiKey(): String {
        val stored = secureStorage.tenorApiKey
        return if (stored.isNotBlank()) stored else BuildConfig.DEFAULT_TENOR_API_KEY
    }

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    suspend fun featured(limit: Int = 24): List<Gif> = request("featured", null, limit)

    suspend fun search(query: String, limit: Int = 24): List<Gif> =
        if (query.isBlank()) featured(limit) else request("search", query, limit)

    private suspend fun request(path: String, query: String?, limit: Int): List<Gif> =
        withContext(Dispatchers.IO) {
            val key = apiKey()
            if (key.isBlank()) return@withContext emptyList()
            val encoded = query?.let { URLEncoder.encode(it, "UTF-8") }
            val url = buildString {
                append(Constants.TENOR_BASE_URL).append(path)
                append("?key=").append(key)
                append("&limit=").append(limit)
                append("&media_filter=tinygif,gif")
                append("&client_key=customboard")
                if (encoded != null) append("&q=").append(encoded)
            }
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use emptyList()
                    parse(response.body?.string().orEmpty())
                }
            }.getOrDefault(emptyList())
        }

    private fun parse(body: String): List<Gif> = runCatching {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        val gifs = ArrayList<Gif>(results.length())
        for (index in 0 until results.length()) {
            val item = results.getJSONObject(index)
            val formats = item.optJSONObject("media_formats") ?: continue
            val preview = formats.optJSONObject("tinygif")?.optString("url").orEmpty()
            val full = formats.optJSONObject("gif")?.optString("url").orEmpty()
            if (preview.isEmpty() || full.isEmpty()) continue
            gifs += Gif(
                id = item.optString("id"),
                previewUrl = preview,
                fullUrl = full,
                description = item.optString("content_description")
            )
        }
        gifs
    }.getOrDefault(emptyList())

    suspend fun thumbnail(url: String): Bitmap? = withContext(Dispatchers.IO) {
        thumbnailCache.get(url)?.let { return@withContext it }
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@use null
                val options = BitmapFactory.Options().apply { inSampleSize = 1 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also {
                    thumbnailCache.put(url, it)
                }
            }
        }.getOrNull()
    }

    /** Downloads the full GIF into the cache so it can be shared through the file provider. */
    suspend fun download(gif: Gif): File? = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "gifs").apply { mkdirs() }
            val file = File(directory, "${gif.id.ifBlank { System.currentTimeMillis() }}.gif")
            if (file.exists() && file.length() > 0) return@withContext file
            client.newCall(Request.Builder().url(gif.fullUrl).build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: return@use null
                file.writeBytes(bytes)
                file
            }
        }.getOrNull()
    }

    fun clearCache() {
        thumbnailCache.evictAll()
        runCatching { File(context.cacheDir, "gifs").deleteRecursively() }
    }
}
