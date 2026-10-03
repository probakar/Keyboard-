package com.customboard.keyboard.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import com.customboard.keyboard.R

/** A [SeekBarPreference] that appends a unit (%, dp, ms...) to the displayed value. */
class SeekBarWithValuePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.seekBarPreferenceStyle
) : SeekBarPreference(context, attrs, defStyleAttr) {

    private var unit: String = ""

    init {
        val array = context.obtainStyledAttributes(attrs, R.styleable.SeekBarWithValuePreference)
        unit = array.getString(R.styleable.SeekBarWithValuePreference_valueUnit).orEmpty()
        array.recycle()
        showSeekBarValue = false
        isAdjustable = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val valueView = holder.findViewById(androidx.preference.R.id.seekbar_value)
        if (valueView is android.widget.TextView) {
            valueView.visibility = android.view.View.VISIBLE
            valueView.text = context.getString(R.string.value_with_unit, value, unit)
        }
    }
}
