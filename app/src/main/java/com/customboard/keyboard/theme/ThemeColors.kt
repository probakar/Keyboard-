package com.customboard.keyboard.theme

import android.graphics.Color
import androidx.annotation.ColorInt
import com.customboard.keyboard.utils.blendWith
import com.customboard.keyboard.utils.contrastingTextColor
import com.customboard.keyboard.utils.withAlpha

/**
 * Every colour the keyboard needs. Themes are plain data so they can be created at runtime
 * (custom themes, Material You, wallpaper extraction) and applied without recreating views.
 */
data class ThemeColors(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    @ColorInt val background: Int,
    @ColorInt val backgroundEnd: Int,
    @ColorInt val keyBackground: Int,
    @ColorInt val keyPressed: Int,
    @ColorInt val keySpecial: Int,
    @ColorInt val keyAccent: Int,
    @ColorInt val keyText: Int,
    @ColorInt val keySecondaryText: Int,
    @ColorInt val keyAccentText: Int,
    @ColorInt val popupBackground: Int,
    @ColorInt val popupText: Int,
    @ColorInt val suggestionText: Int,
    @ColorInt val suggestionHighlight: Int,
    @ColorInt val border: Int,
    @ColorInt val accent: Int,
    @ColorInt val gestureTrail: Int,
    val hasGradient: Boolean = false
) {

    /** Returns a copy with the background alpha set from a 20..100 percentage. */
    fun withOpacity(percent: Int): ThemeColors {
        if (percent >= 100) return this
        val alpha = (255 * percent / 100f).toInt().coerceIn(40, 255)
        return copy(
            background = background.withAlpha(alpha),
            backgroundEnd = backgroundEnd.withAlpha(alpha),
            keyBackground = keyBackground.withAlpha((alpha + 30).coerceAtMost(255)),
            keySpecial = keySpecial.withAlpha((alpha + 20).coerceAtMost(255))
        )
    }

    /** Stronger separation between keys and text for the accessibility high-contrast mode. */
    fun highContrast(): ThemeColors = if (isDark) {
        copy(
            background = Color.BLACK,
            backgroundEnd = Color.BLACK,
            keyBackground = Color.BLACK,
            keySpecial = Color.parseColor("#111111"),
            keyText = Color.WHITE,
            keySecondaryText = Color.WHITE,
            border = Color.WHITE,
            suggestionText = Color.WHITE
        )
    } else {
        copy(
            background = Color.WHITE,
            backgroundEnd = Color.WHITE,
            keyBackground = Color.WHITE,
            keySpecial = Color.parseColor("#EEEEEE"),
            keyText = Color.BLACK,
            keySecondaryText = Color.BLACK,
            border = Color.BLACK,
            suggestionText = Color.BLACK
        )
    }

    companion object {

        /** Builds a complete, readable theme from just a few seed colours. */
        fun fromSeeds(
            id: String,
            displayName: String,
            @ColorInt background: Int,
            @ColorInt keyBackground: Int,
            @ColorInt accent: Int,
            @ColorInt keyText: Int = keyBackground.contrastingTextColor(),
            @ColorInt backgroundEnd: Int = background,
            hasGradient: Boolean = false
        ): ThemeColors {
            val dark = background.let {
                (0.299f * Color.red(it) + 0.587f * Color.green(it) + 0.114f * Color.blue(it)) / 255f < 0.5f
            }
            val pressed = if (dark) keyBackground.blendWith(Color.WHITE, 0.18f)
            else keyBackground.blendWith(Color.BLACK, 0.12f)
            val special = if (dark) keyBackground.blendWith(Color.BLACK, 0.35f)
            else keyBackground.blendWith(background, 0.72f)
            return ThemeColors(
                id = id,
                displayName = displayName,
                isDark = dark,
                background = background,
                backgroundEnd = backgroundEnd,
                keyBackground = keyBackground,
                keyPressed = pressed,
                keySpecial = special,
                keyAccent = accent,
                keyText = keyText,
                keySecondaryText = keyText.withAlpha(165),
                keyAccentText = accent.contrastingTextColor(),
                popupBackground = if (dark) keyBackground.blendWith(Color.WHITE, 0.08f)
                else Color.WHITE,
                popupText = keyText,
                suggestionText = keyText,
                suggestionHighlight = accent,
                border = if (dark) Color.parseColor("#4D5156") else Color.parseColor("#DADCE0"),
                accent = accent,
                gestureTrail = accent,
                hasGradient = hasGradient
            )
        }
    }
}
