package com.customboard.keyboard.widgets

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.withAlpha

/** Full-panel voice typing UI: pulsing mic, live partial text and a stop button. */
class VoiceInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onVoiceStopRequested()
        fun onVoiceCancelled()
    }

    var listener: Listener? = null

    private val statusLabel = TextView(context)
    private val partialLabel = TextView(context)
    private val micView = MicView(context)
    private val cancelButton = TextView(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        statusLabel.apply {
            text = context.getString(R.string.voice_listening)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(12f).toInt(), 0, 0)
        }
        addView(statusLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addView(
            micView,
            LayoutParams(context.dpToPx(96f).toInt(), context.dpToPx(96f).toInt()).apply {
                topMargin = context.dpToPx(10f).toInt()
                gravity = Gravity.CENTER_HORIZONTAL
            }
        )
        micView.setOnClickListener { listener?.onVoiceStopRequested() }

        partialLabel.apply {
            textSize = 15f
            gravity = Gravity.CENTER
            maxLines = 3
            setPadding(context.dpToPx(20f).toInt(), context.dpToPx(10f).toInt(),
                context.dpToPx(20f).toInt(), 0)
        }
        addView(partialLabel, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        cancelButton.apply {
            text = context.getString(R.string.action_cancel)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(20f).toInt(), context.dpToPx(10f).toInt(),
                context.dpToPx(20f).toInt(), context.dpToPx(10f).toInt())
            setOnClickListener { listener?.onVoiceCancelled() }
        }
        addView(cancelButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            bottomMargin = context.dpToPx(10f).toInt()
        })
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        statusLabel.setTextColor(theme.keyText)
        partialLabel.setTextColor(theme.keySecondaryText)
        cancelButton.setTextColor(theme.accent)
        micView.accentColor = theme.accent
        micView.invalidate()
    }

    fun setStatus(text: CharSequence) {
        statusLabel.text = text
    }

    fun setPartial(text: CharSequence) {
        partialLabel.text = text
    }

    fun setVolume(level: Float) = micView.setLevel(level)

    fun startPulsing() = micView.start()

    fun stopPulsing() = micView.stop()

    /** Circular microphone indicator that reacts to the input volume. */
    private class MicView(context: Context) : View(context) {

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val icon = androidx.appcompat.content.res.AppCompatResources
            .getDrawable(context, R.drawable.ic_mic)
        private var animator: ValueAnimator? = null
        private var pulse = 0f
        private var level = 0f

        var accentColor: Int = ThemeManager.getInstance(context).current.accent

        init {
            isClickable = true
        }

        fun setLevel(value: Float) {
            level = value.coerceIn(0f, 1f)
            invalidate()
        }

        fun start() {
            stop()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    pulse = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun stop() {
            animator?.cancel()
            animator = null
            pulse = 0f
            invalidate()
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            stop()
        }

        override fun onDraw(canvas: Canvas) {
            val centerX = width / 2f
            val centerY = height / 2f
            val base = minOf(width, height) / 2f * 0.55f

            paint.color = accentColor.withAlpha((46 * (1f - pulse)).toInt())
            canvas.drawCircle(centerX, centerY, base + base * pulse * 0.9f, paint)

            paint.color = accentColor.withAlpha(72)
            canvas.drawCircle(centerX, centerY, base * (1f + level * 0.35f), paint)

            paint.color = accentColor
            canvas.drawCircle(centerX, centerY, base * 0.78f, paint)

            icon?.let {
                val size = (base * 0.9f).toInt()
                it.setBounds(
                    (centerX - size / 2).toInt(), (centerY - size / 2).toInt(),
                    (centerX + size / 2).toInt(), (centerY + size / 2).toInt()
                )
                it.setTint(android.graphics.Color.WHITE)
                it.draw(canvas)
            }
        }
    }
}
