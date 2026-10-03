package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.textprocessing.TextStatistics
import com.customboard.keyboard.textprocessing.TextTransformer
import com.customboard.keyboard.textprocessing.UnicodeStylizer
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx

/** Case conversion, Unicode fonts and text statistics for the current selection. */
class TextToolsPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onTextToolsReplace(text: String)
        fun onTextToolsClosed()
        fun textToolsInput(): String
    }

    var listener: Listener? = null

    private val transformRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val styleList = LinearLayout(context).apply { orientation = VERTICAL }
    private val statsLabel = TextView(context)
    private val closeButton = ImageButton(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    private val transforms = listOf(
        TextTransformer.Transform.UPPERCASE to R.string.transform_uppercase,
        TextTransformer.Transform.LOWERCASE to R.string.transform_lowercase,
        TextTransformer.Transform.TITLE_CASE to R.string.transform_title_case,
        TextTransformer.Transform.SENTENCE_CASE to R.string.transform_sentence_case,
        TextTransformer.Transform.TOGGLE_CASE to R.string.transform_toggle_case,
        TextTransformer.Transform.CAMEL_CASE to R.string.transform_camel_case,
        TextTransformer.Transform.SNAKE_CASE to R.string.transform_snake_case,
        TextTransformer.Transform.KEBAB_CASE to R.string.transform_kebab_case,
        TextTransformer.Transform.REVERSE to R.string.transform_reverse,
        TextTransformer.Transform.TRIM_SPACES to R.string.transform_trim_spaces,
        TextTransformer.Transform.REMOVE_LINE_BREAKS to R.string.transform_remove_breaks,
        TextTransformer.Transform.SORT_LINES to R.string.transform_sort_lines,
        TextTransformer.Transform.REMOVE_DUPLICATE_LINES to R.string.transform_remove_duplicates,
        TextTransformer.Transform.ADD_QUOTES to R.string.transform_add_quotes,
        TextTransformer.Transform.URL_ENCODE to R.string.transform_url_encode
    )

    init {
        orientation = VERTICAL
        buildHeader()
        buildTransforms()
        buildStyles()
        buildFooter()
    }

    private fun buildHeader() {
        statsLabel.apply {
            textSize = 12f
            setPadding(context.dpToPx(14f).toInt(), context.dpToPx(8f).toInt(), 0, context.dpToPx(4f).toInt())
        }
        addView(statsLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    private fun buildTransforms() {
        transforms.forEach { (transform, titleRes) ->
            transformRow.addView(chip(context.getString(titleRes)) {
                applyToSelection { text -> TextTransformer.apply(text, transform) }
            })
        }
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                transformRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    private fun buildStyles() {
        val scroll = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                styleList,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun buildFooter() {
        val footer = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { listener?.onTextToolsClosed() }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    private fun chip(label: String, onClick: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 12f
        gravity = Gravity.CENTER
        setPadding(context.dpToPx(12f).toInt(), context.dpToPx(6f).toInt(),
            context.dpToPx(12f).toInt(), context.dpToPx(6f).toInt())
        background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = context.dpToPx(6f).toInt()
            topMargin = context.dpToPx(5f).toInt()
            bottomMargin = context.dpToPx(5f).toInt()
        }
        setOnClickListener { onClick() }
    }

    private fun applyToSelection(transform: (String) -> String) {
        val input = listener?.textToolsInput().orEmpty()
        if (input.isBlank()) return
        listener?.onTextToolsReplace(transform(input))
        refresh()
    }

    /** Rebuilds the previews for the current selection. */
    fun refresh() {
        val input = listener?.textToolsInput().orEmpty()
        val sample = input.ifBlank { context.getString(R.string.text_tools_sample) }
        val stats = TextStatistics.of(input)
        statsLabel.text = context.getString(
            R.string.text_tools_stats, stats.words, stats.characters, stats.sentences
        )
        styleList.removeAllViews()
        UnicodeStylizer.allStyles(sample.take(40)).forEach { (style, preview) ->
            val row = TextView(context).apply {
                text = preview
                textSize = 16f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(context.dpToPx(14f).toInt(), context.dpToPx(10f).toInt(),
                    context.dpToPx(14f).toInt(), context.dpToPx(10f).toInt())
                background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
                setOnClickListener {
                    if (input.isNotBlank()) {
                        listener?.onTextToolsReplace(UnicodeStylizer.apply(input, style))
                    }
                }
            }
            styleList.addView(
                row,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
        }
        applyTheme(theme)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        statsLabel.setTextColor(theme.keySecondaryText)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until transformRow.childCount) {
            val chip = transformRow.getChildAt(index) as TextView
            chip.setTextColor(theme.keyText)
            chip.backgroundTintList =
                android.content.res.ColorStateList.valueOf(theme.keyBackground)
        }
        for (index in 0 until styleList.childCount) {
            (styleList.getChildAt(index) as TextView).setTextColor(theme.keyText)
        }
    }
}
