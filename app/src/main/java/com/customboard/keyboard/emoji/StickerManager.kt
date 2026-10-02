package com.customboard.keyboard.emoji

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/**
 * Built-in sticker packs.
 *
 * Android apps receive stickers either as rich content (images) or as text. CustomBoard ships
 * compact text based sticker packs that work in every app, including those that reject rich
 * content, and are instantly shareable.
 */
class StickerManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    data class StickerPack(val id: String, val title: String, val stickers: List<String>)

    val packs: List<StickerPack> = listOf(
        StickerPack(
            "reactions", "Reactions",
            listOf(
                "👍 Nice!", "🔥 On fire!", "😂 LOL", "🎉 Congrats!", "💯 Perfect",
                "🙌 Well done!", "👏 Bravo", "🤯 Mind blown", "🥳 Let's celebrate",
                "😍 Love it", "🤝 Deal", "✅ Done"
            )
        ),
        StickerPack(
            "greetings", "Greetings",
            listOf(
                "👋 Hello!", "🌞 Good morning!", "🌙 Good night!", "☕ Coffee time",
                "😊 How are you?", "🙏 Thank you!", "💐 Take care", "✈️ Safe travels",
                "🎂 Happy birthday!", "🎊 Happy new year!"
            )
        ),
        StickerPack(
            "work", "Work",
            listOf(
                "📅 Let's schedule it", "📝 Noted", "⏰ On my way", "📎 See attached",
                "✅ Approved", "🚧 In progress", "📊 Report ready", "🤖 Automating this",
                "☑️ Task complete", "🧠 Thinking about it"
            )
        ),
        StickerPack(
            "love", "Love",
            listOf(
                "❤️ Love you", "💕 Miss you", "🥰 You're the best", "🌹 For you",
                "💌 Sending love", "🤗 Virtual hug", "😘 Kisses", "💞 Always"
            )
        )
    )

    fun allStickers(): List<String> = packs.flatMap { it.stickers }

    fun packById(id: String): StickerPack? = packs.firstOrNull { it.id == id }

    fun track(sticker: String) {
        if (prefs.incognito) return
        RecentEmojiTracker(contextRef).track(sticker.take(2).trim())
    }

    private val contextRef = context.applicationContext
}
