package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.toolbar.ToolbarItem
import com.customboard.keyboard.toolbar.ToolbarManager
import com.customboard.keyboard.utils.dpToPx

/** Gboard-style expanded function drawer, driven by the user's toolbar visibility and order. */
class ToolbarPanelView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onToolbarItemSelected(item: ToolbarItem)
        fun onToolbarCustomizeRequested()
        fun onToolbarPanelClosed()
    }

    var listener: Listener? = null

    private val manager = ToolbarManager(context)
    private val header = LinearLayout(context)
    private val title = TextView(context)
    private val closeButton = ImageButton(context)
    private val rows = LinearLayout(context)
    private val status = TextView(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current

    init {
        orientation = VERTICAL
        setPadding(context.dpToPx(8f).toInt(), 0, context.dpToPx(8f).toInt(), 0)
        buildHeader()
        val scroll = ScrollView(context).apply {
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(rows, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        buildFooter()
        refresh()
    }

    private fun buildHeader() {
        header.orientation = HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        title.apply {
            text = context.getString(R.string.toolbar_panel_title)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpToPx(8f).toInt(), 0, 0, 0)
        }
        header.addView(title, LayoutParams(0, context.dpToPx(46f).toInt(), 1f))
        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { listener?.onToolbarPanelClosed() }
        }
        header.addView(closeButton, LayoutParams(context.dpToPx(44f).toInt(), context.dpToPx(44f).toInt()))
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(48f).toInt()))
    }

    private fun buildFooter() {
        status.apply {
            text = context.getString(R.string.toolbar_customize_hint)
            textSize = 11f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpToPx(8f).toInt(), 0, context.dpToPx(8f).toInt(), 0)
        }
        addView(status, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(34f).toInt()))
        val customize = TextView(context).apply {
            text = context.getString(R.string.toolbar_customize)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(12f).toInt(), 0, context.dpToPx(12f).toInt(), 0)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            setOnClickListener { listener?.onToolbarCustomizeRequested() }
        }
        addView(customize, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    fun refresh() {
        rows.removeAllViews()
        val items = manager.visibleItems()
        if (items.isEmpty()) {
            val empty = TextView(context).apply {
                text = context.getString(R.string.toolbar_no_actions)
                gravity = Gravity.CENTER
            }
            rows.addView(empty, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(76f).toInt()))
        } else {
            items.chunked(COLUMN_COUNT).forEach { chunk ->
                val row = LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                chunk.forEach { item -> row.addView(createAction(item), itemLayoutParams()) }
                repeat(COLUMN_COUNT - chunk.size) { row.addView(View(context), itemLayoutParams()) }
                rows.addView(row, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(82f).toInt()))
            }
        }
        applyTheme(theme)
    }

    private fun itemLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
            val margin = context.dpToPx(3f).toInt()
            setMargins(margin, margin, margin, margin)
        }

    private fun createAction(item: ToolbarItem): View = LinearLayout(context).apply {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        contentDescription = context.getString(item.titleRes)
        background = GradientDrawable().apply {
            cornerRadius = context.dpToPx(14f)
            setColor(theme.keyBackground)
        }
        val icon = ImageView(context).apply {
            setImageResource(item.iconRes)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            contentDescription = null
        }
        addView(icon, LayoutParams(context.dpToPx(26f).toInt(), context.dpToPx(26f).toInt()))
        val label = TextView(context).apply {
            setText(item.titleRes)
            textSize = 10.5f
            gravity = Gravity.CENTER
            maxLines = 2
            setPadding(context.dpToPx(2f).toInt(), context.dpToPx(2f).toInt(), context.dpToPx(2f).toInt(), 0)
        }
        addView(label, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        setOnClickListener { listener?.onToolbarItemSelected(item) }
        tag = listOf(icon, label)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        title.setTextColor(theme.keyText)
        status.setTextColor(theme.keySecondaryText)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (rowIndex in 0 until rows.childCount) {
            val row = rows.getChildAt(rowIndex) as? LinearLayout ?: continue
            for (cellIndex in 0 until row.childCount) {
                val cell = row.getChildAt(cellIndex) as? LinearLayout ?: continue
                val views = cell.tag as? List<*> ?: continue
                val icon = views.getOrNull(0) as? ImageView ?: continue
                val label = views.getOrNull(1) as? TextView ?: continue
                icon.setColorFilter(theme.accent)
                label.setTextColor(theme.keyText)
                (cell.background as? GradientDrawable)?.setColor(theme.keyBackground)
            }
        }
    }

    companion object {
        private const val COLUMN_COUNT = 4
    }
}
