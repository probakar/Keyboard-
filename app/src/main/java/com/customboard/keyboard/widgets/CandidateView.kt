package com.customboard.keyboard.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.autocorrect.Suggestion
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.setVisible
import com.customboard.keyboard.utils.visible

/**
 * The suggestion strip above the keys: word candidates, smart replies, the inline calculator
 * result and the "paste" chip, exactly like Gboard's.
 */
class CandidateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onSuggestionPicked(suggestion: Suggestion)
        fun onSuggestionLongPressed(suggestion: Suggestion)
        fun onExpandSuggestions(suggestions: List<Suggestion>)
        fun onCandidateMenuClicked()
    }

    var listener: Listener? = null

    private val scrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val expandButton = ImageButton(context).apply {
        setImageResource(R.drawable.ic_expand_more)
        background = null
        contentDescription = context.getString(R.string.cd_expand_suggestions)
    }
    private val menuButton = ImageButton(context).apply {
        setImageResource(R.drawable.ic_more_horiz)
        background = null
        contentDescription = context.getString(R.string.cd_more_options)
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var current: List<Suggestion> = emptyList()
    private var showDividers = true

    init {
        addView(
            scrollView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                marginEnd = context.dpToPx(44f).toInt()
            }
        )
        scrollView.addView(
            container,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
        addView(
            expandButton,
            LayoutParams(context.dpToPx(40f).toInt(), LayoutParams.MATCH_PARENT, Gravity.END)
        )
        addView(
            menuButton,
            LayoutParams(context.dpToPx(40f).toInt(), LayoutParams.MATCH_PARENT, Gravity.END)
        )
        menuButton.gone()
        expandButton.setOnClickListener { listener?.onExpandSuggestions(current) }
        menuButton.setOnClickListener { listener?.onCandidateMenuClicked() }
        setWillNotDraw(false)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        expandButton.setColorFilter(theme.keySecondaryText)
        menuButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until container.childCount) {
            styleChip(container.getChildAt(index) as TextView, index)
        }
        invalidate()
    }

    fun setSuggestions(suggestions: List<Suggestion>) {
        current = suggestions
        container.removeAllViews()
        if (suggestions.isEmpty()) {
            expandButton.gone()
            invalidate()
            return
        }
        expandButton.setVisible(suggestions.size > 3)
        val width = (resources.displayMetrics.widthPixels - context.dpToPx(44f)) / 3f
        suggestions.forEachIndexed { index, suggestion ->
            container.addView(createChip(suggestion, index, width.toInt()))
        }
        invalidate()
    }

    /** Non-word chips: smart replies, clipboard paste, calculator results. */
    fun setChips(labels: List<String>, source: Suggestion.Source, icon: Int? = null) {
        setSuggestions(labels.map { Suggestion(it, 0.0, source) })
        showDividers = false
        if (icon != null && container.childCount > 0) {
            (container.getChildAt(0) as TextView).setCompoundDrawablesRelativeWithIntrinsicBounds(
                icon, 0, 0, 0
            )
        }
    }

    fun clear() {
        current = emptyList()
        container.removeAllViews()
        showDividers = true
        expandButton.gone()
        invalidate()
    }

    val isEmpty: Boolean get() = current.isEmpty()

    @SuppressLint("ClickableViewAccessibility")
    private fun createChip(suggestion: Suggestion, index: Int, width: Int): TextView {
        val chip = TextView(context).apply {
            text = suggestion.word
            gravity = Gravity.CENTER
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(context.dpToPx(10f).toInt(), 0, context.dpToPx(10f).toInt(), 0)
            minWidth = width
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
            setOnClickListener { listener?.onSuggestionPicked(suggestion) }
            setOnLongClickListener {
                listener?.onSuggestionLongPressed(suggestion)
                true
            }
        }
        styleChip(chip, index)
        return chip
    }

    private fun styleChip(chip: TextView, index: Int) {
        val suggestion = current.getOrNull(index)
        val highlighted = suggestion?.isAutoCorrection == true ||
            suggestion?.source == Suggestion.Source.AI
        chip.setTextColor(if (highlighted) theme.suggestionHighlight else theme.suggestionText)
        chip.typeface = if (highlighted) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        chip.textSize = 16f
        chip.compoundDrawablePadding = context.dpToPx(6f).toInt()
        chip.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(
            if (highlighted) theme.suggestionHighlight else theme.suggestionText
        )
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (showDividers && container.childCount > 1) {
            dividerPaint.color = theme.border
            dividerPaint.strokeWidth = context.dpToPx(1f)
            val top = height * 0.25f
            val bottom = height * 0.75f
            for (index in 1 until container.childCount) {
                val child = container.getChildAt(index)
                val x = child.left - scrollView.scrollX.toFloat()
                if (x > 0 && x < width) canvas.drawLine(x, top, x, bottom, dividerPaint)
            }
        }
        super.dispatchDraw(canvas)
    }

    fun showMenuButton(show: Boolean) {
        menuButton.setVisible(show)
        if (show) expandButton.gone() else expandButton.visible()
    }
}
