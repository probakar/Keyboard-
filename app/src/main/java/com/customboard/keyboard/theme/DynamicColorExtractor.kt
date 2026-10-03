package com.customboard.keyboard.theme

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.palette.graphics.Palette
import com.customboard.keyboard.utils.blendWith
import com.customboard.keyboard.utils.contrastingTextColor
import com.customboard.keyboard.utils.isDarkColor

/**
 * Material You / wallpaper based theming.
 *
 * * Android 12+ uses the system dynamic palette (`android.R.color.system_accent*`).
 * * Android 8.1–11 falls back to [WallpaperManager.getWallpaperColors].
 * * Anything older (or any failure) falls back to the built-in Gboard palette.
 */
object DynamicColorExtractor {

    fun extract(context: Context, dark: Boolean): ThemeColors {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                return fromSystemPalette(context, dark)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                fromWallpaperColors(context, dark)?.let { return it }
            }
        }
        return if (dark) ThemePresets.DARK else ThemePresets.LIGHT
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun fromSystemPalette(context: Context, dark: Boolean): ThemeColors {
        val res = context.resources
        return if (dark) {
            val background = res.getColor(android.R.color.system_neutral1_900, null)
            val key = res.getColor(android.R.color.system_neutral1_800, null)
            val accent = res.getColor(android.R.color.system_accent1_200, null)
            ThemeColors.fromSeeds(
                ThemePresets.ID_MATERIAL_YOU, "Material You",
                background = background,
                keyBackground = key,
                accent = accent,
                keyText = res.getColor(android.R.color.system_neutral1_50, null)
            )
        } else {
            val background = res.getColor(android.R.color.system_accent2_100, null)
            val key = res.getColor(android.R.color.system_neutral1_50, null)
            val accent = res.getColor(android.R.color.system_accent1_600, null)
            ThemeColors.fromSeeds(
                ThemePresets.ID_MATERIAL_YOU, "Material You",
                background = background,
                keyBackground = key,
                accent = accent,
                keyText = res.getColor(android.R.color.system_neutral1_900, null)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun fromWallpaperColors(context: Context, dark: Boolean): ThemeColors? {
        val manager = WallpaperManager.getInstance(context) ?: return null
        val colors = manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
        val primary = colors.primaryColor.toArgb()
        val secondary = colors.secondaryColor?.toArgb() ?: primary
        return buildFromSeed(primary, secondary, dark)
    }

    /** Extracts a palette from an arbitrary bitmap (used for custom background images). */
    fun fromBitmap(bitmap: Bitmap, dark: Boolean): ThemeColors {
        val palette = Palette.from(bitmap).clearFilters().generate()
        val primary = palette.getDominantColor(if (dark) Color.DKGRAY else Color.LTGRAY)
        val accent = palette.getVibrantColor(palette.getMutedColor(primary))
        return buildFromSeed(primary, accent, dark)
    }

    private fun buildFromSeed(primary: Int, accent: Int, dark: Boolean): ThemeColors {
        val background = if (dark) primary.blendWith(Color.BLACK, 0.72f)
        else primary.blendWith(Color.WHITE, 0.78f)
        val key = if (dark) primary.blendWith(Color.BLACK, 0.5f)
        else primary.blendWith(Color.WHITE, 0.94f)
        val resolvedAccent = if (dark && accent.isDarkColor()) accent.blendWith(Color.WHITE, 0.4f)
        else accent
        return ThemeColors.fromSeeds(
            ThemePresets.ID_MATERIAL_YOU,
            "Material You",
            background = background,
            keyBackground = key,
            accent = resolvedAccent,
            keyText = key.contrastingTextColor()
        )
    }
}
