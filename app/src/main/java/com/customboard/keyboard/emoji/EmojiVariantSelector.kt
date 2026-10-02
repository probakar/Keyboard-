package com.customboard.keyboard.emoji

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/** Skin tone and gender variants for the emoji that support them. */
class EmojiVariantSelector(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    /** 0 = default yellow, 1..5 = Fitzpatrick types 1-2 .. 6. */
    var skinTone: Int
        get() = prefs.skinTone
        set(value) {
            prefs.skinTone = value.coerceIn(0, 5)
        }

    fun applyPreferredTone(item: EmojiItem): String =
        if (item.supportsSkinTone) applyTone(item.emoji, skinTone) else item.emoji

    fun applyTone(emoji: String, tone: Int): String {
        val stripped = stripTone(emoji)
        if (tone <= 0 || tone > MODIFIERS.size) return stripped
        return stripped + MODIFIERS[tone - 1]
    }

    fun stripTone(emoji: String): String {
        var result = emoji
        MODIFIERS.forEach { result = result.replace(it, "") }
        return result
    }

    /** All five tone variants, used by the long-press popup. */
    fun variantsOf(emoji: String): List<String> {
        val base = stripTone(emoji)
        return listOf(base) + MODIFIERS.map { base + it }
    }

    fun genderVariants(emoji: String): List<String> {
        val base = stripTone(emoji)
        return GENDER_PAIRS[base]?.toList() ?: emptyList()
    }

    companion object {
        /** U+1F3FB .. U+1F3FF */
        val MODIFIERS = listOf("\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")

        private val GENDER_PAIRS = mapOf(
            "🧑" to listOf("👨", "👩"),
            "🧒" to listOf("👦", "👧"),
            "🧓" to listOf("👴", "👵"),
            "🙋" to listOf("🙋‍♂️", "🙋‍♀️"),
            "🤷" to listOf("🤷‍♂️", "🤷‍♀️"),
            "🤦" to listOf("🤦‍♂️", "🤦‍♀️"),
            "💆" to listOf("💆‍♂️", "💆‍♀️"),
            "🏃" to listOf("🏃‍♂️", "🏃‍♀️"),
            "🚶" to listOf("🚶‍♂️", "🚶‍♀️")
        )
    }
}
