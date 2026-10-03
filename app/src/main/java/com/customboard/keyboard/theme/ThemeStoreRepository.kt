package com.customboard.keyboard.theme

import android.content.Context
import com.customboard.keyboard.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Loads the community theme catalog online and falls back to the packaged catalog offline. */
object ThemeStoreRepository {

    data class StoreTheme(
        val id: String,
        val name: String,
        val description: String,
        val previewColor: Int,
        val source: String
    )

    data class Catalog(val themes: List<StoreTheme>, val isOnline: Boolean)

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val catalogBaseUrl: String
        get() = Constants.THEME_STORE_CATALOG_URL.substringBeforeLast('/')

    /** Call on an IO dispatcher. */
    fun loadCatalog(context: Context): Catalog {
        val remote = runCatching { get(Constants.THEME_STORE_CATALOG_URL) }
            .getOrNull()
            ?.let { parseCatalog(it, online = true) }
            ?.takeIf { it.isNotEmpty() }
        if (remote != null) return Catalog(remote, isOnline = true)

        val bundled = runCatching {
            context.applicationContext.assets.open("theme-store/catalog.json")
                .bufferedReader().use { it.readText().take(MAX_JSON_CHARS) }
        }.getOrNull()?.let { parseCatalog(it, online = false) }.orEmpty()
        return Catalog(bundled, isOnline = false)
    }

    /** Call on an IO dispatcher. Accepts only repository-hosted HTTPS URLs or packaged assets. */
    fun loadThemeJson(context: Context, theme: StoreTheme): String? = runCatching {
        if (theme.source.startsWith(ASSET_PREFIX)) {
            val assetPath = theme.source.removePrefix(ASSET_PREFIX)
            context.applicationContext.assets.open(assetPath).bufferedReader()
                .use { it.readText().take(MAX_JSON_CHARS) }
        } else {
            get(theme.source)
        }
    }.getOrNull()

    private fun parseCatalog(raw: String, online: Boolean): List<StoreTheme> = runCatching {
        val entries = JSONObject(raw).optJSONArray("themes") ?: return emptyList()
        buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val id = item.optString("id")
                val name = item.optString("name").take(40)
                val relativePath = item.optString("theme").trim()
                if (!id.matches(ID_PATTERN) || name.isBlank() || relativePath.isBlank()) continue
                val source = if (relativePath.startsWith("https://")) {
                    if (!relativePath.startsWith(RAW_THEMES_PREFIX)) continue
                    relativePath
                } else if (online) {
                    val path = relativePath.removePrefix("/")
                    if (path.contains("..")) continue
                    "$catalogBaseUrl/$path"
                } else {
                    val path = relativePath.removePrefix("/").removePrefix("theme-store/")
                    "$ASSET_PREFIX" + "theme-store/$path"
                }
                val preview = runCatching {
                    android.graphics.Color.parseColor(item.optString("previewColor", "#6750A4"))
                }.getOrDefault(android.graphics.Color.rgb(103, 80, 164))
                add(
                    StoreTheme(
                        id = id,
                        name = name,
                        description = item.optString("description").take(180),
                        previewColor = preview,
                        source = source
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun get(url: String): String {
        require(url.startsWith("https://")) { "Theme catalog requires HTTPS" }
        val host = java.net.URI(url).host
        require(host == RAW_HOST) { "Untrusted theme host" }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "CustomBoard-ThemeStore")
            .build()
        return client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Theme download failed: ${response.code}" }
            val text = response.body?.string().orEmpty()
            require(text.length <= MAX_JSON_CHARS) { "Theme JSON is too large" }
            text
        }
    }

    private const val ASSET_PREFIX = "asset://"
    private const val RAW_HOST = "raw.githubusercontent.com"
    private const val RAW_THEMES_PREFIX =
        "https://raw.githubusercontent.com/probakar/Keyboard-/arena/01a10043-keyboard/theme-store/"
    private const val MAX_JSON_CHARS = 64 * 1024
    private val ID_PATTERN = Regex("[a-z0-9][a-z0-9-]{1,47}")
}
