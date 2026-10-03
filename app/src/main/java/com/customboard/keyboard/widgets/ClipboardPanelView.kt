package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.customboard.keyboard.R
import com.customboard.keyboard.clipboard.ClipboardEntity
import com.customboard.keyboard.clipboard.ClipboardManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible
import com.customboard.keyboard.utils.withAlpha
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Clipboard history: search, categories, pinning, editing and deleting. */
class ClipboardPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextInputTarget {

    interface Listener {
        fun onClipPicked(text: String)
        fun onClipboardPanelClosed()
        fun onClipboardSettingsRequested()
    }

    var listener: Listener? = null

    private val clipboardManager = ClipboardManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val adapter = ClipAdapter()

    private val searchField = EditText(context)
    private val list = RecyclerView(context)
    private val emptyLabel = TextView(context)
    private val chipRow = LinearLayout(context)
    private val closeButton = ImageButton(context)
    private val clearButton = ImageButton(context)
    private val settingsButton = ImageButton(context)

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var category = ClipboardManager.CATEGORY_ALL
    private var searching = false

    init {
        orientation = VERTICAL
        buildHeader()
        buildList()
        buildFooter()
        reload()
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            text = context.getString(R.string.clipboard_title)
            textSize = 15f
            setPadding(context.dpToPx(14f).toInt(), 0, 0, 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        searchField.apply {
            hint = context.getString(R.string.clipboard_search_hint)
            setSingleLine()
            background = null
            textSize = 14f
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true
            gone()
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) =
                    reload(s?.toString().orEmpty())

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        header.addView(
            searchField,
            LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        )

        val searchButton = ImageButton(context).apply {
            setImageResource(R.drawable.ic_search)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.clipboard_search_hint)
            setOnClickListener { toggleSearch(!searching) }
        }
        header.addView(
            searchButton,
            LinearLayout.LayoutParams(context.dpToPx(40f).toInt(), LayoutParams.MATCH_PARENT)
        )

        settingsButton.apply {
            setImageResource(R.drawable.ic_settings)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.clipboard_settings)
            setOnClickListener { listener?.onClipboardSettingsRequested() }
        }
        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(context.dpToPx(40f).toInt(), LayoutParams.MATCH_PARENT)
        )
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))

        chipRow.orientation = HORIZONTAL
        chipRow.gravity = Gravity.CENTER_VERTICAL
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                chipRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(36f).toInt()))
        buildChips()
    }

    private fun buildChips() {
        chipRow.removeAllViews()
        val categories = listOf(
            ClipboardManager.CATEGORY_ALL to R.string.clipboard_category_all,
            ClipboardEntity.CATEGORY_TEXT to R.string.clipboard_category_text,
            ClipboardEntity.CATEGORY_LINK to R.string.clipboard_category_link,
            ClipboardEntity.CATEGORY_EMAIL to R.string.clipboard_category_email,
            ClipboardEntity.CATEGORY_PHONE to R.string.clipboard_category_phone,
            ClipboardEntity.CATEGORY_NUMBER to R.string.clipboard_category_number,
            ClipboardEntity.CATEGORY_CODE to R.string.clipboard_category_code,
            ClipboardEntity.CATEGORY_ADDRESS to R.string.clipboard_category_address
        )
        categories.forEach { (id, titleRes) ->
            val chip = TextView(context).apply {
                text = context.getString(titleRes)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(context.dpToPx(12f).toInt(), context.dpToPx(4f).toInt(),
                    context.dpToPx(12f).toInt(), context.dpToPx(4f).toInt())
                background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
                tag = id
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = context.dpToPx(6f).toInt()
                    topMargin = context.dpToPx(4f).toInt()
                    bottomMargin = context.dpToPx(4f).toInt()
                }
                setOnClickListener {
                    category = id
                    reload()
                }
            }
            chipRow.addView(chip)
        }
    }

    private fun buildList() {
        list.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        list.adapter = adapter
        list.overScrollMode = View.OVER_SCROLL_NEVER
        val holder = FrameLayout(context)
        holder.addView(
            list,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        emptyLabel.apply {
            text = context.getString(R.string.clipboard_empty)
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(24f).toInt(), 0, context.dpToPx(24f).toInt(), 0)
        }
        holder.addView(
            emptyLabel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        addView(holder, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun buildFooter() {
        val footer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { listener?.onClipboardPanelClosed() }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )
        footer.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        clearButton.apply {
            setImageResource(R.drawable.ic_delete_sweep)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.clipboard_clear_all)
            setOnClickListener { clipboardManager.clearAll { reload() } }
        }
        footer.addView(
            clearButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        adapter.theme = theme
        adapter.notifyDataSetChanged()
        emptyLabel.setTextColor(theme.keySecondaryText)
        searchField.setTextColor(theme.keyText)
        searchField.setHintTextColor(theme.keySecondaryText)
        listOf(closeButton, clearButton, settingsButton).forEach {
            it.setColorFilter(theme.keySecondaryText)
        }
        for (index in 0 until chipRow.childCount) {
            val chip = chipRow.getChildAt(index) as TextView
            val selected = chip.tag == category
            chip.setTextColor(if (selected) theme.keyAccentText else theme.keyText)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (selected) theme.accent else theme.keyBackground
            )
        }
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child is LinearLayout && child.childCount > 0) {
                val first = child.getChildAt(0)
                if (first is TextView) first.setTextColor(theme.keyText)
            }
        }
    }

    fun reload(query: String = searchField.text?.toString().orEmpty()) {
        scope.launch {
            val items = when {
                query.isNotBlank() -> clipboardManager.search(query)
                else -> clipboardManager.byCategory(category)
            }
            adapter.submit(items)
            if (items.isEmpty()) emptyLabel.visible() else emptyLabel.gone()
            applyTheme(theme)
        }
    }

    private fun toggleSearch(enable: Boolean) {
        searching = enable
        if (enable) {
            searchField.visible()
            searchField.requestFocus()
        } else {
            searchField.setText("")
            searchField.gone()
        }
    }

    override val isAcceptingText: Boolean get() = searching

    override fun onTextCommitted(text: String) {
        if (!searching) return
        searchField.append(text)
        searchField.setSelection(searchField.text?.length ?: 0)
    }

    override fun onBackspacePressed() {
        if (!searching) return
        val value = searchField.text?.toString().orEmpty()
        if (value.isNotEmpty()) searchField.setText(value.dropLast(1))
        searchField.setSelection(searchField.text?.length ?: 0)
    }

    override fun onEnterPressed() {
        adapter.firstItem()?.let { listener?.onClipPicked(it.content) }
    }

    private inner class ClipAdapter : RecyclerView.Adapter<ClipAdapter.Holder>() {

        private val items = ArrayList<ClipboardEntity>()
        var theme: ThemeColors = ThemeManager.getInstance(context).current

        fun submit(newItems: List<ClipboardEntity>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun firstItem(): ClipboardEntity? = items.firstOrNull()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_clipboard, parent, false)
            )

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.text.text = item.preview.ifBlank { item.content }
            holder.text.setTextColor(theme.keyText)
            holder.time.text = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT
            ).format(Date(item.timestamp))
            holder.time.setTextColor(theme.keySecondaryText)
            holder.card.backgroundTintList =
                android.content.res.ColorStateList.valueOf(theme.keyBackground)
            holder.pin.setImageResource(
                if (item.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin
            )
            holder.pin.setColorFilter(if (item.isPinned) theme.accent else theme.keySecondaryText)
            holder.delete.setColorFilter(theme.keySecondaryText.withAlpha(205))
            holder.category.text = item.category
            holder.category.setTextColor(theme.keySecondaryText)

            holder.itemView.setOnClickListener { listener?.onClipPicked(item.content) }
            holder.pin.setOnClickListener {
                clipboardManager.togglePin(item)
                reload()
            }
            holder.delete.setOnClickListener {
                clipboardManager.delete(item)
                reload()
            }
        }

        override fun getItemCount(): Int = items.size

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val card: View = view.findViewById(R.id.clip_card)
            val text: TextView = view.findViewById(R.id.clip_text)
            val time: TextView = view.findViewById(R.id.clip_time)
            val category: TextView = view.findViewById(R.id.clip_category)
            val pin: ImageView = view.findViewById(R.id.clip_pin)
            val delete: ImageView = view.findViewById(R.id.clip_delete)
        }
    }
}
