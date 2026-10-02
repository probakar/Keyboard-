package com.customboard.keyboard.search

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.customboard.keyboard.R
import java.net.URLEncoder

/** Opens a web search for the selected or typed text. */
class WebSearchManager(private val context: Context) {

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
}
