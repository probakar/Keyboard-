package com.customboard.keyboard.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.utils.dpToPx

/**
 * Draws miniature keyboards. Used by the theme picker so a theme can be previewed before it
 * is applied, and by the settings "live preview" card.
 */
class KeyboardRenderer(private val context: Context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }

    private val rows = listOf(10, 9, 9, 5)

    fun renderPreview(theme: ThemeColors, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888
        )
        draw(Canvas(bitmap), theme, width.toFloat(), height.toFloat())
        return bitmap
    }

    fun draw(canvas: Canvas, theme: ThemeColors, width: Float, height: Float) {
        if (theme.hasGradient && theme.background != theme.backgroundEnd) {
            paint.shader = LinearGradient(
                0f, 0f, 0f, height, theme.background, theme.backgroundEnd, Shader.TileMode.CLAMP
            )
        } else {
            paint.shader = null
            paint.color = theme.background
        }
        canvas.drawRect(0f, 0f, width, height, paint)
        paint.shader = null

        val padding = context.dpToPx(4f)
        val gap = context.dpToPx(2.5f)
        val rowHeight = (height - padding * 2 - gap * (rows.size - 1)) / rows.size
        val radius = context.dpToPx(3f)
        textPaint.textSize = rowHeight * 0.52f

        var y = padding
        rows.forEachIndexed { rowIndex, count ->
            val keyWidth = (width - padding * 2 - gap * (count - 1)) / count
            var x = padding
            for (column in 0 until count) {
                val special = rowIndex == 3 && (column == 0 || column == count - 1) ||
                    rowIndex == 2 && (column == 0 || column == count - 1)
                paint.color = when {
                    rowIndex == 3 && column == count - 1 -> theme.keyAccent
                    special -> theme.keySpecial
                    else -> theme.keyBackground
                }
                val keyW = if (rowIndex == 3 && column == 2) keyWidth * 2.2f else keyWidth
                val rect = RectF(x, y, x + keyW, y + rowHeight)
                canvas.drawRoundRect(rect, radius, radius, paint)
                if (rowIndex == 0 && column < PREVIEW_LETTERS.length) {
                    textPaint.color = theme.keyText
                    val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(
                        PREVIEW_LETTERS[column].toString(), rect.centerX(), baseline, textPaint
                    )
                }
                x += keyW + gap
            }
            y += rowHeight + gap
        }
    }

    companion object {
        private const val PREVIEW_LETTERS = "qwertyuiop"
    }
}
