package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.AnimationUtils
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.spToPx

/**
 * A single themed, key-shaped button. The main keyboard draws its keys on one canvas for
 * speed; this view is used wherever individual keys are needed (emoji panel, cursor pad,
 * sticker packs, AI actions) so everything keeps the same look and feel.
 */
class CustomKeyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = context.spToPx(18f)
    }
    private val rect = RectF()

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var pressedState = false

    var label: String? = null
        set(value) {
            field = value
            contentDescription = value
            invalidate()
        }

    var icon: Drawable? = null
        set(value) {
            field = value
            invalidate()
        }

    var accent: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var flat: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var cornerRadius: Float = context.dpToPx(10f)
        set(value) {
            field = value
            invalidate()
        }

    var textSizeSp: Float = 18f
        set(value) {
            field = value
            textPaint.textSize = context.spToPx(value)
            invalidate()
        }

    var onClick: (() -> Unit)? = null
    var onLongClick: (() -> Unit)? = null

    init {
        isClickable = true
        isFocusable = true
        setPadding(0, 0, 0, 0)
    }

    fun setIconResource(resId: Int) {
        icon = AppCompatResources.getDrawable(context, resId)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val inset = context.dpToPx(2f)
        rect.set(inset, inset, width - inset, height - inset)

        if (!flat) {
            backgroundPaint.color = when {
                pressedState -> theme.keyPressed
                accent -> theme.keyAccent
                else -> theme.keyBackground
            }
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
        } else if (pressedState) {
            backgroundPaint.color = theme.keyPressed
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, backgroundPaint)
        }

        val foreground = if (accent && !flat) theme.keyAccentText else theme.keyText

        icon?.let { drawable ->
            val size = (minOf(rect.width(), rect.height()) * 0.46f).toInt()
            val left = (width - size) / 2
            val top = (height - size) / 2
            drawable.setBounds(left, top, left + size, top + size)
            drawable.setTint(foreground)
            drawable.draw(canvas)
            return
        }

        label?.let { text ->
            textPaint.color = foreground
            val baseline = rect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
            canvas.drawText(text, rect.centerX(), baseline, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedState = true
                AnimationUtils.pressScale(this, true)
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                pressedState = false
                AnimationUtils.pressScale(this, false)
                invalidate()
                if (isInside(event)) performClick()
            }

            MotionEvent.ACTION_CANCEL -> {
                pressedState = false
                AnimationUtils.pressScale(this, false)
                invalidate()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        onClick?.invoke()
        return true
    }

    override fun performLongClick(): Boolean {
        onLongClick?.let {
            it.invoke()
            return true
        }
        return super.performLongClick()
    }

    private fun isInside(event: MotionEvent): Boolean =
        event.x >= 0 && event.y >= 0 && event.x <= width && event.y <= height
}
