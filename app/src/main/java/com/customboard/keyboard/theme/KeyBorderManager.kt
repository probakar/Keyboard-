package com.customboard.keyboard.theme

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.dpToPx

/** Resolves the key shape / border settings into concrete drawing values. */
class KeyBorderManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    enum class Shape { ROUNDED, PILL, SQUARE, CIRCLE, FLAT }

    val shape: Shape
        get() = when (prefs.keyShape) {
            "pill" -> Shape.PILL
            "square" -> Shape.SQUARE
            "circle" -> Shape.CIRCLE
            "flat" -> Shape.FLAT
            else -> Shape.ROUNDED
        }

    val showBorders: Boolean get() = prefs.showKeyBorders || prefs.highContrast

    val borderWidthPx: Float
        get() = context.dpToPx(if (prefs.highContrast) 2f else 1f)

    /** Corner radius in pixels for a key of the given height. */
    fun cornerRadiusPx(keyHeightPx: Float): Float = when (shape) {
        Shape.SQUARE, Shape.FLAT -> 0f
        Shape.PILL, Shape.CIRCLE -> keyHeightPx / 2f
        Shape.ROUNDED -> context.dpToPx(prefs.keyCornerRadiusDp.toFloat())
    }

    /** Keys are drawn as circles only when they are roughly square already. */
    fun shouldForceSquare(): Boolean = shape == Shape.CIRCLE

    fun drawsKeyBackground(): Boolean = shape != Shape.FLAT
}
