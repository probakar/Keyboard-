package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import com.customboard.keyboard.R
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.KeyCodes
import com.customboard.keyboard.utils.dpToPx

/** Arrow pad with selection and clipboard shortcuts, like Samsung's cursor control panel. */
class CursorControlPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun interface Listener {
        fun onCursorAction(code: Int)
    }

    var listener: Listener? = null

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private val keys = ArrayList<CustomKeyView>()

    init {
        orientation = VERTICAL
        setPadding(
            context.dpToPx(8f).toInt(), context.dpToPx(6f).toInt(),
            context.dpToPx(8f).toInt(), context.dpToPx(6f).toInt()
        )
        addView(
            row(
                KeyCodes.SELECT_ALL to R.string.toolbar_select_all,
                KeyCodes.CUT to R.string.toolbar_cut,
                KeyCodes.COPY to R.string.toolbar_copy,
                KeyCodes.PASTE to R.string.toolbar_paste
            ),
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
        addView(arrowRow(), LayoutParams(LayoutParams.MATCH_PARENT, 0, 2.4f))
        addView(
            row(
                KeyCodes.UNDO to R.string.toolbar_undo,
                KeyCodes.REDO to R.string.toolbar_redo,
                KeyCodes.DELETE_WORD to R.string.cursor_delete_word,
                KeyCodes.BACK_TO_KEYBOARD to R.string.cd_back_to_keyboard
            ),
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
    }

    private fun row(vararg entries: Pair<Int, Int>): LinearLayout {
        val row = LinearLayout(context).apply { orientation = HORIZONTAL }
        entries.forEach { (code, titleRes) ->
            row.addView(
                key(context.getString(titleRes), code),
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
            )
        }
        return row
    }

    private fun arrowRow(): LinearLayout {
        val wrapper = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
        }
        val left = key("\u25C0", KeyCodes.CURSOR_LEFT)
        val right = key("\u25B6", KeyCodes.CURSOR_RIGHT)
        val middle = LinearLayout(context).apply { orientation = VERTICAL }
        middle.addView(
            key("\u25B2", KeyCodes.CURSOR_UP),
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
        middle.addView(
            key("\u25BC", KeyCodes.CURSOR_DOWN),
            LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        )
        wrapper.addView(left, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        wrapper.addView(middle, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        wrapper.addView(right, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        return wrapper
    }

    private fun key(label: String, code: Int): CustomKeyView =
        CustomKeyView(context).apply {
            this.label = label
            textSizeSp = if (label.length > 2) 12f else 16f
            onClick = { listener?.onCursorAction(code) }
            keys += this
        }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        keys.forEach { it.applyTheme(theme) }
    }
}
