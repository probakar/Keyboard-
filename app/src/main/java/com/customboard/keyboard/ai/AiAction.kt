package com.customboard.keyboard.ai

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.customboard.keyboard.R

/** Every AI action offered by the keyboard's AI panel. */
enum class AiAction(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val needsSelection: Boolean,
    val isCloud: Boolean
) {
    PROOFREAD("proofread", R.string.ai_action_proofread, R.drawable.ic_ai_proofread, true, true),
    REWRITE("rewrite", R.string.ai_action_rewrite, R.drawable.ic_ai_rewrite, true, true),
    TONE("tone", R.string.ai_action_tone, R.drawable.ic_ai_tone, true, true),
    SUMMARIZE("summarize", R.string.ai_action_summarize, R.drawable.ic_ai_summarize, true, true),
    EXPAND("expand", R.string.ai_action_expand, R.drawable.ic_ai_expand, true, true),
    SHORTEN("shorten", R.string.ai_action_shorten, R.drawable.ic_ai_shorten, true, true),
    BULLETS("bullets", R.string.ai_action_bullets, R.drawable.ic_ai_bullets, true, true),
    TRANSLATE("translate", R.string.ai_action_translate, R.drawable.ic_translate, true, false),
    REPLY("reply", R.string.ai_action_reply, R.drawable.ic_ai_reply, false, false),
    CONTINUE("continue", R.string.ai_action_continue, R.drawable.ic_ai_continue, false, true),
    EMOJIFY("emojify", R.string.ai_action_emojify, R.drawable.ic_ai_emoji, true, true),
    EXPLAIN("explain", R.string.ai_action_explain, R.drawable.ic_ai_explain, true, true),
    ASK("ask", R.string.ai_action_ask, R.drawable.ic_ai_ask, false, true),
    COMPOSE("compose", R.string.ai_action_compose, R.drawable.ic_ai_compose, false, true),
    HASHTAGS("hashtags", R.string.ai_action_hashtags, R.drawable.ic_ai_hashtag, true, true),
    CUSTOM("custom", R.string.ai_action_custom, R.drawable.ic_ai_custom, false, true);

    companion object {
        fun fromId(id: String): AiAction = entries.firstOrNull { it.id == id } ?: REWRITE
    }
}
