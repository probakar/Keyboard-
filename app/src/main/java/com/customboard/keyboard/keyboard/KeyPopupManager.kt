package com.customboard.keyboard.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.theme.FontManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.withAlpha
import kotlin.math.ceil
import kotlin.math.min

/**
 * Two popups:
 *  * the enlarged **key preview** shown while a character key is held down, and
 *  * the **alternate characters** popup opened by a long press (à á â …).
 *
 * Both are non-touchable [PopupWindow]s so the keyboard keeps receiving the gesture; the
 * selected alternate is tracked from the finger position reported by the keyboard view.
 */
class KeyPopupManager(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)

    private var theme: ThemeColors = ThemePresets.LIGHT

    private var previewWindow: PopupWindow? = null
    private var previewView: PreviewView? = null

    private var alternatesWindow: PopupWindow? = null
    private var alternatesView: AlternatesView? = null
    private var alternatesOrigin = 0f
    private var alternatesTop = 0f
    private var cellWidth = 0f
    private var cellHeight = 0f
    private var columns = 0
    private var rows = 1
    private var characters: List<String> = emptyList()
    private var selectedIndex = 0

    val isShowingAlternates: Boolean get() = alternatesWindow?.isShowing == true

    fun setTheme(newTheme: ThemeColors) {
        theme = newTheme
        previewView?.invalidate()
        alternatesView?.invalidate()
    }

    // ------------------------------------------------------------------
    //  Key preview
    // ------------------------------------------------------------------

    fun showPreview(anchor: View, key: Key, rect: RectF, uppercase: Boolean) {
        if (!prefs.keyPreview || !key.isCharacter) return
        val label = if (uppercase) key.label.uppercase() else key.label
        val view = previewView ?: PreviewView(context).also { previewView = it }
        view.text = label
        view.theme = theme

        val width = (rect.width() * 1.25f).toInt().coerceAtLeast(context.dpToPx(44f).toInt())
        val height = (rect.height() * 1.3f).toInt()
        val window = previewWindow ?: PopupWindow(view, width, height).apply {
            isTouchable = false
            isFocusable = false
            isClippingEnabled = false
            previewWindow = this
        }
        window.width = width
        window.height = height
        val x = (rect.centerX() - width / 2f).toInt() + anchorLeft(anchor)
        val y = (rect.top - height - context.dpToPx(6f)).toInt() + anchorTop(anchor)
        if (window.isShowing) {
            window.update(x, y, width, height)
        } else {
            runCatching { window.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y) }
        }
        view.invalidate()
    }

    fun hidePreview() {
        runCatching { previewWindow?.dismiss() }
    }

    // ------------------------------------------------------------------
    //  Long press alternates
    // ------------------------------------------------------------------

    /** @return true when a popup was actually shown. */
    fun showAlternates(anchor: View, key: Key, rect: RectF, uppercase: Boolean): Boolean {
        val base = key.popupKeys
        if (base.isEmpty()) return false
        characters = if (uppercase) base.map { it.uppercase() } else base
        selectedIndex = 0

        cellWidth = context.dpToPx(44f)
        cellHeight = context.dpToPx(48f)
        columns = min(characters.size, 6)
        rows = ceil(characters.size / columns.toFloat()).toInt().coerceAtLeast(1)

        val width = (cellWidth * columns + context.dpToPx(10f)).toInt()
        val height = (cellHeight * rows + context.dpToPx(10f)).toInt()

        val view = alternatesView ?: AlternatesView(context).also { alternatesView = it }
        view.theme = theme
        view.items = characters
        view.columns = columns
        view.selected = 0
        view.cellWidth = cellWidth
        view.cellHeight = cellHeight

        var x = rect.centerX() - width / 2f
        x = x.coerceIn(0f, (anchor.width - width).coerceAtLeast(0).toFloat())
        val y = rect.top - height - context.dpToPx(4f)

        alternatesOrigin = x + context.dpToPx(5f)
        alternatesTop = y + context.dpToPx(5f)

        val window = alternatesWindow ?: PopupWindow(view, width, height).apply {
            isTouchable = false
            isFocusable = false
            isClippingEnabled = false
            alternatesWindow = this
        }
        window.width = width
        window.height = height
        val screenX = x.toInt() + anchorLeft(anchor)
        val screenY = y.toInt() + anchorTop(anchor)
        runCatching {
            if (window.isShowing) {
                window.update(screenX, screenY, width, height)
            } else {
                window.showAtLocation(anchor, Gravity.NO_GRAVITY, screenX, screenY)
            }
        }
        view.invalidate()
        return true
    }

    /** Updates the highlighted alternate from the finger position (keyboard coordinates). */
    fun updateAlternates(x: Float, y: Float) {
        if (!isShowingAlternates || characters.isEmpty()) return
        val column = ((x - alternatesOrigin) / cellWidth).toInt().coerceIn(0, columns - 1)
        val row = ((y - alternatesTop) / cellHeight).toInt().coerceIn(0, rows - 1)
        val index = (row * columns + column).coerceIn(0, characters.size - 1)
        if (index != selectedIndex) {
            selectedIndex = index
            alternatesView?.selected = index
            alternatesView?.invalidate()
        }
    }

    /** Dismisses the popup and returns the selected character. */
    fun commitAlternates(): String? {
        if (!isShowingAlternates) return null
        val value = characters.getOrNull(selectedIndex)
        dismissAlternates()
        return value
    }

    fun dismissAlternates() {
        runCatching { alternatesWindow?.dismiss() }
    }

    fun dismissAll() {
        hidePreview()
        dismissAlternates()
    }

    private fun anchorTop(anchor: View): Int {
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        return location[1]
    }

    private fun anchorLeft(anchor: View): Int {
        val location = IntArray(2)
        anchor.getLocationInWindow(location)
        return location[0]
    }

    // ------------------------------------------------------------------
    //  Views
    // ------------------------------------------------------------------

    private class PreviewView(context: Context) : View(context) {
        var text: String = ""
        var theme: ThemeColors = ThemePresets.LIGHT

        private val background = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            val radius = context.dpToPx(10f)
            background.color = theme.popupBackground
            canvas.drawRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, background
            )
            background.color = theme.border.withAlpha(60)
            textPaint.typeface = FontManager.typeface(context)
            textPaint.color = theme.popupText
            textPaint.textSize = height * 0.5f
            val baseline = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, width / 2f, baseline, textPaint)
        }
    }

    private class AlternatesView(context: Context) : View(context) {
        var items: List<String> = emptyList()
        var columns: Int = 1
        var selected: Int = 0
        var cellWidth: Float = 0f
        var cellHeight: Float = 0f
        var theme: ThemeColors = ThemePresets.LIGHT

        private val background = Paint(Paint.ANTI_ALIAS_FLAG)
        private val highlight = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        override fun onDraw(canvas: Canvas) {
            val radius = context.dpToPx(12f)
            background.color = theme.popupBackground
            canvas.drawRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, background
            )
            val padding = context.dpToPx(5f)
            textPaint.typeface = FontManager.typeface(context)
            textPaint.textSize = context.dpToPx(19f)

            items.forEachIndexed { index, item ->
                val column = index % columns
                val row = index / columns
                val rect = RectF(
                    padding + column * cellWidth,
                    padding + row * cellHeight,
                    padding + (column + 1) * cellWidth,
                    padding + (row + 1) * cellHeight
                )
                if (index == selected) {
                    highlight.color = theme.accent
                    canvas.drawRoundRect(
                        RectF(
                            rect.left + 2f, rect.top + 2f, rect.right - 2f, rect.bottom - 2f
                        ),
                        context.dpToPx(8f), context.dpToPx(8f), highlight
                    )
                    textPaint.color = theme.keyAccentText
                } else {
                    textPaint.color = theme.popupText
                }
                val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
                canvas.drawText(item, rect.centerX(), baseline, textPaint)
            }
        }
    }
}
