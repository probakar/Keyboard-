package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.customboard.keyboard.R
import com.customboard.keyboard.emoji.GifSearchManager
import com.customboard.keyboard.emoji.StickerManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Stickers and GIFs in one panel with two tabs. */
class MediaPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextInputTarget {

    enum class Tab { STICKERS, GIFS }

    interface Listener {
        fun onStickerPicked(sticker: String)
        fun onGifPicked(gif: GifSearchManager.Gif)
        fun onMediaPanelClosed()
        fun onMediaSettingsRequested()
    }

    var listener: Listener? = null

    private val stickerManager = StickerManager(context)
    private val gifManager = GifSearchManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadJob: Job? = null

    private val tabRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val searchField = EditText(context)
    private val grid = RecyclerView(context)
    private val message = TextView(context)
    private val poweredBy = TextView(context)
    private val closeButton = ImageButton(context)

    private val stickerAdapter = StickerAdapter()
    private val gifAdapter = GifAdapter()

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var tab = Tab.STICKERS
    private var searchFocused = false

    init {
        orientation = VERTICAL
        buildTabs()
        buildSearch()
        buildGrid()
        buildFooter()
        showTab(Tab.STICKERS)
    }

    private fun buildTabs() {
        listOf(Tab.STICKERS to R.string.media_stickers, Tab.GIFS to R.string.media_gifs)
            .forEach { (value, titleRes) ->
                val tabView = TextView(context).apply {
                    text = context.getString(titleRes)
                    textSize = 14f
                    gravity = Gravity.CENTER
                    tag = value.name
                    background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
                    setOnClickListener { showTab(value) }
                }
                tabRow.addView(tabView, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            }
        addView(tabRow, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    private fun buildSearch() {
        searchField.apply {
            hint = context.getString(R.string.media_search_hint)
            setSingleLine()
            textSize = 14f
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_field)
            setPadding(context.dpToPx(12f).toInt(), context.dpToPx(6f).toInt(),
                context.dpToPx(12f).toInt(), context.dpToPx(6f).toInt())
            setOnFocusChangeListener { _, hasFocus -> searchFocused = hasFocus }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                    reload(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        addView(
            searchField,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = context.dpToPx(10f).toInt()
                marginEnd = context.dpToPx(10f).toInt()
                topMargin = context.dpToPx(4f).toInt()
            }
        )
    }

    private fun buildGrid() {
        grid.layoutManager = GridLayoutManager(context, 2)
        grid.overScrollMode = View.OVER_SCROLL_NEVER
        val holder = FrameLayout(context)
        holder.addView(
            grid,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        message.apply {
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(context.dpToPx(24f).toInt(), 0, context.dpToPx(24f).toInt(), 0)
            gone()
        }
        holder.addView(
            message,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        addView(holder, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun buildFooter() {
        val footer = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener { listener?.onMediaPanelClosed() }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )
        poweredBy.apply {
            text = context.getString(R.string.media_gif_powered_by)
            textSize = 11f
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            setPadding(0, 0, context.dpToPx(12f).toInt(), 0)
            visibility = View.GONE
        }
        footer.addView(poweredBy, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    fun showTab(value: Tab) {
        tab = value
        poweredBy.visibility = if (value == Tab.GIFS) View.VISIBLE else View.GONE
        searchField.setText("")
        grid.adapter = if (value == Tab.STICKERS) stickerAdapter else gifAdapter
        (grid.layoutManager as GridLayoutManager).spanCount = if (value == Tab.STICKERS) 2 else 2
        reload("")
        applyTheme(theme)
    }

    private fun reload(query: String) {
        loadJob?.cancel()
        message.gone()
        when (tab) {
            Tab.STICKERS -> {
                val items = if (query.isBlank()) stickerManager.allStickers()
                else stickerManager.allStickers().filter { it.contains(query, ignoreCase = true) }
                stickerAdapter.submit(items)
                if (items.isEmpty()) showMessage(context.getString(R.string.media_no_results))
            }

            Tab.GIFS -> {
                if (!gifManager.hasApiKey()) {
                    gifAdapter.submit(emptyList())
                    showMessage(context.getString(R.string.media_gif_key_missing))
                    message.setOnClickListener { listener?.onMediaSettingsRequested() }
                    message.isClickable = true
                    return
                }
                showMessage(context.getString(R.string.media_loading))
                loadJob = scope.launch {
                    val results = gifManager.search(query)
                    gifAdapter.submit(results)
                    if (results.isEmpty()) showMessage(context.getString(R.string.media_no_results))
                    else message.gone()
                }
            }
        }
    }

    private fun showMessage(text: String) {
        message.setOnClickListener(null)
        message.isClickable = false
        message.text = text
        message.visible()
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        message.setTextColor(theme.keySecondaryText)
        poweredBy.setTextColor(theme.keySecondaryText)
        searchField.setTextColor(theme.keyText)
        searchField.setHintTextColor(theme.keySecondaryText)
        searchField.backgroundTintList =
            android.content.res.ColorStateList.valueOf(theme.keyBackground)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until tabRow.childCount) {
            val view = tabRow.getChildAt(index) as TextView
            view.setTextColor(if (view.tag == tab.name) theme.accent else theme.keySecondaryText)
        }
        stickerAdapter.notifyDataSetChanged()
    }

    override val isAcceptingText: Boolean get() = searchFocused

    override fun onTextCommitted(text: String) {
        if (!searchFocused) return
        searchField.append(text)
        searchField.setSelection(searchField.text?.length ?: 0)
    }

    override fun onBackspacePressed() {
        if (!searchFocused) return
        val value = searchField.text?.toString().orEmpty()
        if (value.isNotEmpty()) searchField.setText(value.dropLast(1))
        searchField.setSelection(searchField.text?.length ?: 0)
    }

    override fun onEnterPressed() = reload(searchField.text?.toString().orEmpty())

    private inner class StickerAdapter : RecyclerView.Adapter<StickerAdapter.Holder>() {
        private val items = ArrayList<String>()

        fun submit(values: List<String>) {
            items.clear()
            items.addAll(values)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = TextView(parent.context).apply {
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(context.dpToPx(10f).toInt(), context.dpToPx(18f).toInt(),
                    context.dpToPx(10f).toInt(), context.dpToPx(18f).toInt())
                background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT
                )
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val sticker = items[position]
            holder.textView.text = sticker
            holder.textView.setTextColor(theme.keyText)
            holder.textView.setOnClickListener {
                stickerManager.track(sticker)
                listener?.onStickerPicked(sticker)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class Holder(val textView: TextView) : RecyclerView.ViewHolder(textView)
    }

    private inner class GifAdapter : RecyclerView.Adapter<GifAdapter.Holder>() {
        private val items = ArrayList<GifSearchManager.Gif>()

        fun submit(values: List<GifSearchManager.Gif>) {
            items.clear()
            items.addAll(values)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = ImageView(parent.context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
                layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, context.dpToPx(96f).toInt()
                ).apply { setMargins(4, 4, 4, 4) }
            }
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val gif = items[position]
            holder.imageView.setImageDrawable(null)
            holder.imageView.contentDescription = gif.description
            holder.imageView.setBackgroundColor(theme.keyBackground)
            holder.imageView.setOnClickListener { listener?.onGifPicked(gif) }
            scope.launch {
                val bitmap = gifManager.thumbnail(gif.previewUrl)
                if (holder.bindingAdapterPosition == position && bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        inner class Holder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
    }
}
