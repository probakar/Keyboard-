package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.autocorrect.Suggestion
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx

/** The expanded suggestion list shown when the arrow on the candidate strip is tapped. */
class SuggestionsPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onExpandedSuggestionPicked(suggestion: Suggestion)
        fun onExpandedSuggestionsClosed()
    }

    var listener: Listener? = null

    private val content = LinearLayout(context).apply { orientation = VERTICAL }
    private val closeButton = ImageButton(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    init {
        orientation = VERTICAL
        val scroll = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                content,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))

        val footer = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        closeButton.apply {
            setImageResource(R.drawable.ic_expand_less)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { listener?.onExpandedSuggestionsClosed() }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    fun setSuggestions(suggestions: List<Suggestion>) {
        content.removeAllViews()
        var row = newRow()
        suggestions.forEachIndexed { index, suggestion ->
            if (index % 3 == 0 && index > 0) {
                content.addView(row)
                row = newRow()
            }
            row.addView(createCell(suggestion), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        while (row.childCount in 1..2) {
            row.addView(View(context), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        if (row.childCount > 0) content.addView(row)
        applyTheme(theme)
    }

    private fun newRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private fun createCell(suggestion: Suggestion) = TextView(context).apply {
        text = suggestion.word
        textSize = 16f
        gravity = Gravity.CENTER
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
        setPadding(context.dpToPx(8f).toInt(), context.dpToPx(12f).toInt(),
            context.dpToPx(8f).toInt(), context.dpToPx(12f).toInt())
        background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
        setOnClickListener { listener?.onExpandedSuggestionPicked(suggestion) }
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (rowIndex in 0 until content.childCount) {
            val row = content.getChildAt(rowIndex) as LinearLayout
            for (cellIndex in 0 until row.childCount) {
                (row.getChildAt(cellIndex) as? TextView)?.setTextColor(theme.suggestionText)
            }
        }
    }
}
