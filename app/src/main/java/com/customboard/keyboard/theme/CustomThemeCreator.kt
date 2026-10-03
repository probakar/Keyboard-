package com.customboard.keyboard.theme

import android.content.Context
import android.graphics.Color
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.blendWith
import com.customboard.keyboard.utils.contrastingTextColor
import com.customboard.keyboard.utils.withAlpha
import org.json.JSONObject

/** Builds, saves and restores user-defined themes (stored as JSON in preferences). */
object CustomThemeCreator {

    data class Draft(
        var background: Int = Color.parseColor("#F1F3F4"),
        var backgroundEnd: Int = Color.parseColor("#F1F3F4"),
        var keyBackground: Int = Color.WHITE,
        var keyText: Int = Color.parseColor("#1F1F1F"),
        var accent: Int = Color.parseColor("#1A73E8"),
        var specialKey: Int = Color.parseColor("#DADCE0"),
        var gradient: Boolean = false,
        var name: String = "My theme"
    )

    fun buildTheme(draft: Draft): ThemeColors {
        val dark = draft.background.let {
            (0.299f * Color.red(it) + 0.587f * Color.green(it) + 0.114f * Color.blue(it)) / 255f < 0.5f
        }
        return ThemeColors(
            id = ThemePresets.ID_CUSTOM,
            displayName = draft.name,
            isDark = dark,
            background = draft.background,
            backgroundEnd = if (draft.gradient) draft.backgroundEnd else draft.background,
            keyBackground = draft.keyBackground,
            keyPressed = if (dark) draft.keyBackground.blendWith(Color.WHITE, 0.2f)
            else draft.keyBackground.blendWith(Color.BLACK, 0.12f),
            keySpecial = draft.specialKey,
            keyAccent = draft.accent,
            keyText = draft.keyText,
            keySecondaryText = draft.keyText.withAlpha(170),
            keyAccentText = draft.accent.contrastingTextColor(),
            popupBackground = draft.keyBackground,
            popupText = draft.keyText,
            suggestionText = draft.keyText,
            suggestionHighlight = draft.accent,
            border = draft.keyText.withAlpha(60),
            accent = draft.accent,
            gestureTrail = draft.accent,
            hasGradient = draft.gradient
        )
    }

    fun save(context: Context, draft: Draft) {
        val json = JSONObject().apply {
            put("name", draft.name)
            put("background", draft.background)
            put("backgroundEnd", draft.backgroundEnd)
            put("keyBackground", draft.keyBackground)
            put("keyText", draft.keyText)
            put("accent", draft.accent)
            put("specialKey", draft.specialKey)
            put("gradient", draft.gradient)
        }
        PreferencesManager.getInstance(context).customThemeJson = json.toString()
    }

    fun loadDraft(context: Context): Draft {
        val raw = PreferencesManager.getInstance(context).customThemeJson ?: return Draft()
        return parseDraft(raw) ?: Draft()
    }

    /** Parses the app's portable JSON theme format, accepting CSS hex or packed ARGB colours. */
    fun parseDraft(raw: String): Draft? = runCatching {
        val json = JSONObject(raw)
        fun color(key: String, fallback: Int): Int {
            val value = json.opt(key) ?: return fallback
            return when (value) {
                is Number -> value.toInt()
                is String -> value.takeIf { it.isNotBlank() }?.let { Color.parseColor(it) } ?: fallback
                else -> fallback
            }
        }
        Draft(
            background = color("background", Color.parseColor("#F1F3F4")),
            backgroundEnd = color("backgroundEnd", color("background", Color.WHITE)),
            keyBackground = color("keyBackground", Color.WHITE),
            keyText = color("keyText", Color.parseColor("#1F1F1F")),
            accent = color("accent", Color.parseColor("#1A73E8")),
            specialKey = color("specialKey", Color.parseColor("#DADCE0")),
            gradient = json.optBoolean("gradient", false),
            name = json.optString("name", "My theme").take(40).ifBlank { "My theme" }
        )
    }.getOrNull()

    /** Saves a validated imported JSON palette to the user's custom theme slot. */
    fun importTheme(context: Context, raw: String): Boolean {
        val draft = parseDraft(raw) ?: return false
        save(context, draft)
        return true
    }

    fun load(context: Context): ThemeColors = buildTheme(loadDraft(context))
}
