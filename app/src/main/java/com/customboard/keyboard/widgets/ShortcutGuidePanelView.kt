package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.utils.dpToPx

/** In-keyboard quick reference for the gestures and editing shortcuts. */
class ShortcutGuidePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onClose: (() -> Unit)? = null

    private val title = TextView(context)
    private val body = TextView(context)
    private val closeButton = ImageButton(context)

    init {
        orientation = VERTICAL
        val header = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = HORIZONTAL
        }
        title.apply {
            setText(R.string.shortcut_cheatsheet_title)
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(context.dpToPx(16f).toInt(), 0, 0, 0)
        }
        header.addView(title, LayoutParams(0, context.dpToPx(52f).toInt(), 1f))
        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { onClose?.invoke() }
        }
        header.addView(closeButton, LayoutParams(context.dpToPx(48f).toInt(), context.dpToPx(48f).toInt()))
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        body.apply {
            setText(R.string.shortcut_cheatsheet_message)
            textSize = 15f
            setLineSpacing(context.dpToPx(5f), 1f)
            setPadding(
                context.dpToPx(18f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(18f).toInt(), context.dpToPx(20f).toInt()
            )
        }
        scroll.addView(body)
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val done = TextView(context).apply {
            setText(R.string.action_done)
            gravity = Gravity.CENTER
            textSize = 15f
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
            setOnClickListener { onClose?.invoke() }
        }
        addView(done, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(48f).toInt()))
    }

    fun applyTheme(theme: ThemeColors) {
        setBackgroundColor(theme.background)
        title.setTextColor(theme.keyText)
        body.setTextColor(theme.keyText)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until childCount) {
            val row = getChildAt(index)
            if (row is TextView && row !== title && row !== body) row.setTextColor(theme.accent)
        }
    }
}
