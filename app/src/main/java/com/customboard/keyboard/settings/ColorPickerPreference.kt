package com.customboard.keyboard.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.customboard.keyboard.R
import com.customboard.keyboard.widgets.ColorPickerView

/** Preference that stores a colour and shows it as a round swatch. */
class ColorPickerPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    private var currentColor: Int = Color.GRAY
    private var defaultColor: Int = Color.GRAY

    init {
        widgetLayoutResource = R.layout.preference_color_swatch
        context.obtainStyledAttributes(attrs, R.styleable.ColorPickerPreference).use { array ->
            defaultColor = array.getColor(
                R.styleable.ColorPickerPreference_defaultColor, Color.GRAY
            )
        }
        currentColor = defaultColor
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        currentColor = getPersistedInt(
            (defaultValue as? Int) ?: defaultColor
        )
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val swatch = holder.findViewById(R.id.color_swatch) as? ImageView ?: return
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentColor)
            setStroke(2, Color.argb(60, 0, 0, 0))
        }
        swatch.setImageDrawable(drawable)
    }

    override fun onClick() {
        val picker = ColorPickerView(context).apply { color = currentColor }
        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(picker)
            .setPositiveButton(R.string.action_save) { _, _ ->
                currentColor = picker.color
                persistInt(currentColor)
                notifyChanged()
                callChangeListener(currentColor)
            }
            .setNeutralButton(R.string.action_reset) { _, _ ->
                currentColor = defaultColor
                persistInt(currentColor)
                notifyChanged()
                callChangeListener(currentColor)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private inline fun <T> android.content.res.TypedArray.use(block: (android.content.res.TypedArray) -> T): T {
        try {
            return block(this)
        } finally {
            recycle()
        }
    }
}
