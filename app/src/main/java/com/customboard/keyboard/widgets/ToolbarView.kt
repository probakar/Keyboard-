package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.toolbar.ToolbarItem
import com.customboard.keyboard.toolbar.ToolbarManager
import com.customboard.keyboard.utils.dpToPx

/** Scrollable, user-customisable toolbar shown above the suggestion strip. */
class ToolbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    fun interface Listener {
        fun onToolbarAction(item: ToolbarItem)
    }

    var listener: Listener? = null
    var onMoreClick: (() -> Unit)? = null

    private val manager = ToolbarManager(context)
    private val scrollView = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private val container = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val moreButton = ImageButton(context).apply {
        setImageResource(R.drawable.ic_expand_more)
        background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
        contentDescription = context.getString(R.string.toolbar_more)
        scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        setOnClickListener { onMoreClick?.invoke() }
    }

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private val activeStates = HashMap<String, Boolean>()

    init {
        val moreWidth = context.dpToPx(42f).toInt()
        addView(
            scrollView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                marginEnd = moreWidth
            }
        )
        scrollView.addView(
            container,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
        addView(moreButton, LayoutParams(moreWidth, LayoutParams.MATCH_PARENT, Gravity.END))
        refresh()
    }

    fun refresh() {
        container.removeAllViews()
        val size = context.dpToPx(40f).toInt()
        manager.visibleItems().take(QUICK_ACTION_COUNT).forEach { item ->
            val button = ImageButton(context).apply {
                setImageResource(item.iconRes)
                background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
                contentDescription = context.getString(item.titleRes)
                scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                setPadding(context.dpToPx(9f).toInt(), context.dpToPx(9f).toInt(),
                    context.dpToPx(9f).toInt(), context.dpToPx(9f).toInt())
                layoutParams = LinearLayout.LayoutParams(size, LinearLayout.LayoutParams.MATCH_PARENT)
                    .apply { marginEnd = context.dpToPx(2f).toInt() }
                tag = item.id
                setOnClickListener { listener?.onToolbarAction(item) }
            }
            container.addView(button)
        }
        applyTheme(theme)
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        moreButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until container.childCount) {
            val button = container.getChildAt(index) as ImageButton
            val active = activeStates[button.tag as? String] == true
            button.setColorFilter(if (active) theme.accent else theme.keySecondaryText)
        }
    }

    /** Highlights toggles that are currently on (incognito, one handed, split...). */
    fun setActive(itemId: String, active: Boolean) {
        activeStates[itemId] = active
        for (index in 0 until container.childCount) {
            val button = container.getChildAt(index) as ImageButton
            if (button.tag == itemId) {
                button.setColorFilter(if (active) theme.accent else theme.keySecondaryText)
            }
        }
    }

    fun scrollToStart() = scrollView.smoothScrollTo(0, 0)

    companion object {
        private const val QUICK_ACTION_COUNT = 5
    }
}
