package com.customboard.keyboard.widgets

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.emoji.EmojiItem
import com.customboard.keyboard.emoji.EmojiManager
import com.customboard.keyboard.emoji.EmojiSearchEngine
import com.customboard.keyboard.emoji.EmojiVariantSelector
import com.customboard.keyboard.emoji.KaomojiManager
import com.customboard.keyboard.emoji.RecentEmojiTracker
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible

/** Full emoji picker: categories, search, recents, skin tones and kaomoji. */
class EmojiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextInputTarget {

    interface Listener {
        fun onEmojiPicked(emoji: String)
        fun onEmojiBackspace()
        fun onEmojiPanelClosed()
    }

    var listener: Listener? = null

    private val emojiManager = EmojiManager.getInstance(context)
    private val searchEngine = EmojiSearchEngine(context)
    private val recents = RecentEmojiTracker(context)
    private val variants = EmojiVariantSelector(context)

    private val tabStrip = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private val tabScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private val grid = RecyclerView(context)
    private val searchField = EditText(context)
    private val backspaceButton = ImageButton(context)
    private val searchButton = ImageButton(context)
    private val closeButton = ImageButton(context)
    private val emptyLabel = TextView(context)

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private val adapter = EmojiAdapter()
    private var currentCategory = EmojiManager.CATEGORY_SMILEYS
    private var searching = false

    init {
        orientation = VERTICAL
        emojiManager.load()
        buildHeader()
        buildGrid()
        buildFooter()
        showCategory(
            if (recents.recent().isNotEmpty()) EmojiManager.CATEGORY_RECENT
            else EmojiManager.CATEGORY_SMILEYS
        )
    }

    private fun buildHeader() {
        val header = FrameLayout(context)
        tabScroll.addView(
            tabStrip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
        )
        header.addView(
            tabScroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { marginEnd = context.dpToPx(44f).toInt() }
        )

        searchButton.apply {
            setImageResource(R.drawable.ic_search)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.emoji_search_hint)
            setOnClickListener { toggleSearch(true) }
        }
        header.addView(
            searchButton,
            FrameLayout.LayoutParams(
                context.dpToPx(42f).toInt(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END
            )
        )

        searchField.apply {
            hint = context.getString(R.string.emoji_search_hint)
            setSingleLine()
            background = null
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true
            textSize = 15f
            setPadding(context.dpToPx(12f).toInt(), 0, context.dpToPx(12f).toInt(), 0)
            gone()
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s?.toString().orEmpty()
                    if (query.isBlank()) {
                        adapter.submit(itemsFor(currentCategory))
                    } else {
                        adapter.submit(searchEngine.search(query))
                    }
                    updateEmptyState()
                }

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        header.addView(
            searchField,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ).apply { marginEnd = context.dpToPx(44f).toInt() }
        )

        addView(
            header,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(42f).toInt())
        )
        buildTabs()
    }

    private fun buildTabs() {
        tabStrip.removeAllViews()
        emojiManager.categories().forEach { category ->
            val tab = TextView(context).apply {
                text = emojiManager.categoryIcon(category)
                textSize = 17f
                gravity = Gravity.CENTER
                contentDescription = context.getString(emojiManager.categoryTitleRes(category))
                background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
                layoutParams = LinearLayout.LayoutParams(
                    context.dpToPx(42f).toInt(), LinearLayout.LayoutParams.MATCH_PARENT
                )
                tag = category
                setOnClickListener { showCategory(category) }
            }
            tabStrip.addView(tab)
        }
    }

    private fun buildGrid() {
        val cell = context.dpToPx(44f).toInt()
        val columns = (resources.displayMetrics.widthPixels / cell).coerceIn(6, 12)
        grid.layoutManager = GridLayoutManager(context, columns)
        grid.adapter = adapter
        grid.setHasFixedSize(true)
        grid.overScrollMode = View.OVER_SCROLL_NEVER

        val holder = FrameLayout(context)
        holder.addView(
            grid,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        emptyLabel.apply {
            text = context.getString(R.string.emoji_no_results)
            gravity = Gravity.CENTER
            gone()
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
            setOnClickListener {
                toggleSearch(false)
                listener?.onEmojiPanelClosed()
            }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )

        val toneButton = TextView(context).apply {
            text = context.getString(R.string.emoji_skin_tone)
            textSize = 13f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            setOnClickListener { showSkinTonePicker(this) }
        }
        footer.addView(
            toneButton,
            LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        )

        backspaceButton.apply {
            setImageResource(R.drawable.ic_backspace)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.key_delete)
            setOnClickListener { listener?.onEmojiBackspace() }
        }
        footer.addView(
            backspaceButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )

        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        adapter.textColor = theme.keyText
        adapter.notifyDataSetChanged()
        searchField.setTextColor(theme.keyText)
        searchField.setHintTextColor(theme.keySecondaryText)
        emptyLabel.setTextColor(theme.keySecondaryText)
        listOf(backspaceButton, searchButton, closeButton).forEach {
            it.setColorFilter(theme.keySecondaryText)
        }
        for (index in 0 until tabStrip.childCount) {
            val tab = tabStrip.getChildAt(index) as TextView
            tab.setTextColor(
                if (tab.tag == currentCategory) theme.accent else theme.keySecondaryText
            )
        }
    }

    fun showCategory(category: String) {
        currentCategory = category
        toggleSearch(false)
        adapter.submit(itemsFor(category))
        grid.scrollToPosition(0)
        applyTheme(theme)
        updateEmptyState()
    }

    private fun itemsFor(category: String): List<EmojiItem> = when (category) {
        EmojiManager.CATEGORY_RECENT -> recents.recentWithFallback().map {
            EmojiItem(it, category, "recent")
        }

        EmojiManager.CATEGORY_KAOMOJI -> KaomojiManager.asEmojiItems()
        else -> emojiManager.emojisFor(category).map {
            if (it.supportsSkinTone) it.copy(emoji = variants.applyPreferredTone(it)) else it
        }
    }

    private fun toggleSearch(enable: Boolean) {
        if (searching == enable) return
        searching = enable
        if (enable) {
            searchField.visible()
            tabScroll.gone()
            searchButton.setImageResource(R.drawable.ic_close)
            searchButton.setOnClickListener { toggleSearch(false) }
            searchField.requestFocus()
        } else {
            searchField.setText("")
            searchField.gone()
            tabScroll.visible()
            searchButton.setImageResource(R.drawable.ic_search)
            searchButton.setOnClickListener { toggleSearch(true) }
            adapter.submit(itemsFor(currentCategory))
        }
        searchButton.setColorFilter(theme.keySecondaryText)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (adapter.itemCount == 0) emptyLabel.visible() else emptyLabel.gone()
    }

    // ------------------------------------------------------------------
    //  TextInputTarget - the service routes typing here while search is open
    // ------------------------------------------------------------------

    override val isAcceptingText: Boolean get() = searching

    override fun onTextCommitted(text: String) {
        if (!searching) return
        searchField.append(text)
    }

    override fun onBackspacePressed() {
        if (!searching) return
        val value = searchField.text?.toString().orEmpty()
        if (value.isNotEmpty()) searchField.setText(value.dropLast(1))
        searchField.setSelection(searchField.text?.length ?: 0)
    }

    override fun onEnterPressed() {
        if (!searching) return
        val first = adapter.firstItem()
        if (first != null) pick(first.emoji)
    }

    private fun showSkinTonePicker(anchor: View) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(theme.popupBackground)
            setPadding(context.dpToPx(6f).toInt(), context.dpToPx(6f).toInt(),
                context.dpToPx(6f).toInt(), context.dpToPx(6f).toInt())
        }
        val popup = PopupWindow(row, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        variants.variantsOf("\uD83D\uDC4D").forEachIndexed { index, emoji ->
            val option = TextView(context).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                setPadding(context.dpToPx(8f).toInt(), context.dpToPx(4f).toInt(),
                    context.dpToPx(8f).toInt(), context.dpToPx(4f).toInt())
                setOnClickListener {
                    variants.skinTone = index
                    popup.dismiss()
                    showCategory(currentCategory)
                }
            }
            row.addView(option)
        }
        popup.elevation = context.dpToPx(8f)
        popup.showAsDropDown(anchor, 0, -context.dpToPx(96f).toInt())
    }

    private fun showVariantPopup(anchor: View, item: EmojiItem) {
        val options = variants.variantsOf(item.emoji) + variants.genderVariants(item.emoji)
        if (options.size <= 1) return
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(theme.popupBackground)
            setPadding(context.dpToPx(6f).toInt(), context.dpToPx(6f).toInt(),
                context.dpToPx(6f).toInt(), context.dpToPx(6f).toInt())
        }
        val popup = PopupWindow(
            row, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true
        )
        options.distinct().forEach { emoji ->
            row.addView(TextView(context).apply {
                text = emoji
                textSize = 24f
                setPadding(context.dpToPx(8f).toInt(), context.dpToPx(4f).toInt(),
                    context.dpToPx(8f).toInt(), context.dpToPx(4f).toInt())
                setOnClickListener {
                    pick(emoji)
                    popup.dismiss()
                }
            })
        }
        popup.elevation = context.dpToPx(8f)
        popup.showAsDropDown(anchor, 0, -anchor.height * 2)
    }

    private fun pick(emoji: String) {
        recents.track(emoji)
        listener?.onEmojiPicked(emoji)
    }

    private inner class EmojiAdapter : RecyclerView.Adapter<EmojiAdapter.Holder>() {

        private val items = ArrayList<EmojiItem>()
        var textColor: Int = Color.BLACK

        fun submit(newItems: List<EmojiItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_emoji, parent, false)
            return Holder(view as TextView)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            holder.textView.text = item.emoji
            holder.textView.setTextColor(textColor)
            holder.textView.textSize = if (item.category == EmojiManager.CATEGORY_KAOMOJI) 13f else 22f
            holder.textView.setOnClickListener { pick(item.emoji) }
            holder.textView.setOnLongClickListener {
                if (item.supportsSkinTone) {
                    showVariantPopup(holder.textView, item)
                    true
                } else {
                    false
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun firstItem(): EmojiItem? = items.firstOrNull()

        inner class Holder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }
}
