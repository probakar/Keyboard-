package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.customboard.keyboard.R
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.isDarkColor

/**
 * Compact HSV colour picker (hue, saturation, value plus a live swatch and the hex value).
 * Used by the custom theme creator.
 */
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val swatch = TextView(context)
    private val hueBar = SeekBar(context)
    private val saturationBar = SeekBar(context)
    private val valueBar = SeekBar(context)
    private val hsv = floatArrayOf(0f, 1f, 1f)

    var onColorChanged: ((Int) -> Unit)? = null

    var color: Int
        get() = Color.HSVToColor(hsv)
        set(value) {
            Color.colorToHSV(value, hsv)
            hueBar.progress = hsv[0].toInt()
            saturationBar.progress = (hsv[1] * 100).toInt()
            valueBar.progress = (hsv[2] * 100).toInt()
            updateSwatch()
        }

    init {
        orientation = VERTICAL
        val padding = context.dpToPx(16f).toInt()
        setPadding(padding, padding, padding, padding)

        swatch.apply {
            gravity = Gravity.CENTER
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, context.dpToPx(22f).toInt(), 0, context.dpToPx(22f).toInt())
        }
        addView(swatch, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        addBar(R.string.color_hue, hueBar, 360)
        addBar(R.string.color_saturation, saturationBar, 100)
        addBar(R.string.color_value, valueBar, 100)
        color = Color.RED
    }

    private fun addBar(labelRes: Int, bar: SeekBar, max: Int) {
        val label = TextView(context).apply {
            setText(labelRes)
            textSize = 12f
            setPadding(0, context.dpToPx(10f).toInt(), 0, 0)
        }
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        bar.max = max
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                when (bar) {
                    hueBar -> hsv[0] = progress.toFloat()
                    saturationBar -> hsv[1] = progress / 100f
                    valueBar -> hsv[2] = progress / 100f
                }
                updateSwatch()
                onColorChanged?.invoke(color)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun updateSwatch() {
        val current = color
        swatch.setBackgroundColor(current)
        swatch.text = String.format("#%06X", 0xFFFFFF and current)
        swatch.setTextColor(
            if (current.isDarkColor()) Color.WHITE else Color.BLACK
        )
    }
}
