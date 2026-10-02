package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.Defaults
import com.customboard.keyboard.utils.dpToPx

/** Lets the user resize the keyboard and adjust the bottom padding without leaving it. */
class ResizePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onKeyboardMetricsChanged()
        fun onResizePanelClosed()
    }

    var listener: Listener? = null

    private val prefs = PreferencesManager.getInstance(context)
    private val heightLabel = TextView(context)
    private val paddingLabel = TextView(context)
    private val heightBar = SeekBar(context)
    private val paddingBar = SeekBar(context)
    private val resetButton = TextView(context)
    private val doneButton = TextView(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    init {
        orientation = VERTICAL
        setPadding(context.dpToPx(16f).toInt(), context.dpToPx(10f).toInt(),
            context.dpToPx(16f).toInt(), context.dpToPx(10f).toInt())

        addLabelled(heightLabel, heightBar, 50, 160, prefs.keyboardHeightPercent) { value ->
            prefs.keyboardHeightPercent = value
            updateLabels()
            listener?.onKeyboardMetricsChanged()
        }
        addLabelled(paddingLabel, paddingBar, 0, 72, prefs.bottomPaddingDp) { value ->
            prefs.bottomPaddingDp = value
            updateLabels()
            listener?.onKeyboardMetricsChanged()
        }

        val buttons = LinearLayout(context).apply { gravity = Gravity.CENTER }
        resetButton.apply {
            text = context.getString(R.string.action_reset)
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(14f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(14f).toInt(), context.dpToPx(8f).toInt())
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
            setOnClickListener {
                prefs.keyboardHeightPercent = Defaults.KEYBOARD_HEIGHT
                prefs.bottomPaddingDp = Defaults.BOTTOM_PADDING
                heightBar.progress = prefs.keyboardHeightPercent - 50
                paddingBar.progress = prefs.bottomPaddingDp
                updateLabels()
                listener?.onKeyboardMetricsChanged()
            }
        }
        doneButton.apply {
            text = context.getString(R.string.action_done)
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(14f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(14f).toInt(), context.dpToPx(8f).toInt())
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
            setOnClickListener { listener?.onResizePanelClosed() }
        }
        buttons.addView(resetButton, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = context.dpToPx(8f).toInt()
        })
        buttons.addView(doneButton, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(buttons, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = context.dpToPx(12f).toInt()
        })
        updateLabels()
    }

    private fun addLabelled(
        label: TextView,
        bar: SeekBar,
        min: Int,
        max: Int,
        value: Int,
        onChange: (Int) -> Unit
    ) {
        label.textSize = 13f
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        bar.max = max - min
        bar.progress = (value - min).coerceIn(0, max - min)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onChange(progress + min)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun updateLabels() {
        heightLabel.text = context.getString(R.string.resize_height, prefs.keyboardHeightPercent)
        paddingLabel.text = context.getString(R.string.resize_bottom_padding, prefs.bottomPaddingDp)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        heightLabel.setTextColor(theme.keyText)
        paddingLabel.setTextColor(theme.keyText)
        listOf(resetButton, doneButton).forEach {
            it.setTextColor(theme.keyText)
            it.backgroundTintList = android.content.res.ColorStateList.valueOf(theme.keyBackground)
        }
        listOf(heightBar, paddingBar).forEach {
            it.progressTintList = android.content.res.ColorStateList.valueOf(theme.accent)
            it.thumbTintList = android.content.res.ColorStateList.valueOf(theme.accent)
        }
    }
}
