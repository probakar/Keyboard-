package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.search.ContactsSearchManager
import com.customboard.keyboard.search.WebSearchManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.visible

/** Web search shortcuts plus contact lookup, driven by the selected or typed text. */
class SearchPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextInputTarget {

    interface Listener {
        fun onSearchPanelClosed()
        fun onSearchInsert(text: String)
        fun searchInitialQuery(): String
    }

    var listener: Listener? = null

    private val webSearch = WebSearchManager(context)
    private val contacts = ContactsSearchManager(context)

    private val queryField = EditText(context)
    private val providerRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val resultList = LinearLayout(context).apply { orientation = VERTICAL }
    private val hintLabel = TextView(context)
    private val closeButton = ImageButton(context)
    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var queryFocused = true

    init {
        orientation = VERTICAL
        buildQuery()
        buildProviders()
        buildResults()
        buildFooter()
    }

    private fun buildQuery() {
        queryField.apply {
            hint = context.getString(R.string.search_hint)
            setSingleLine()
            textSize = 15f
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_field)
            setPadding(context.dpToPx(12f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(12f).toInt(), context.dpToPx(8f).toInt())
            setOnFocusChangeListener { _, hasFocus -> queryFocused = hasFocus }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) =
                    refreshContacts(s?.toString().orEmpty())

                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        addView(
            queryField,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = context.dpToPx(10f).toInt()
                marginEnd = context.dpToPx(10f).toInt()
                topMargin = context.dpToPx(8f).toInt()
            }
        )
    }

    private fun buildProviders() {
        val providers = listOf(
            WebSearchManager.Provider.GOOGLE to R.string.search_google,
            WebSearchManager.Provider.IMAGES to R.string.search_images,
            WebSearchManager.Provider.MAPS to R.string.search_maps,
            WebSearchManager.Provider.YOUTUBE to R.string.search_youtube,
            WebSearchManager.Provider.TRANSLATE to R.string.search_translate,
            WebSearchManager.Provider.DEFINITION to R.string.search_definition
        )
        providers.forEach { (provider, titleRes) ->
            providerRow.addView(chip(context.getString(titleRes)) {
                webSearch.search(currentQuery(), provider)
            })
        }
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                providerRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(46f).toInt()))
    }

    private fun buildResults() {
        hintLabel.apply {
            textSize = 12f
            setPadding(context.dpToPx(14f).toInt(), context.dpToPx(6f).toInt(), 0, 0)
            text = context.getString(R.string.search_contacts_hint)
        }
        addView(hintLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(context).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                resultList,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
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
            setOnClickListener { listener?.onSearchPanelClosed() }
        }
        footer.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(48f).toInt(), LayoutParams.MATCH_PARENT)
        )
        val shareButton = TextView(context).apply {
            text = context.getString(R.string.action_share)
            textSize = 13f
            gravity = Gravity.CENTER
            setOnClickListener { webSearch.share(currentQuery()) }
        }
        footer.addView(shareButton, LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(footer, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(44f).toInt()))
    }

    private fun chip(label: String, onClick: () -> Unit) = TextView(context).apply {
        text = label
        textSize = 12f
        gravity = Gravity.CENTER
        setPadding(context.dpToPx(14f).toInt(), context.dpToPx(7f).toInt(),
            context.dpToPx(14f).toInt(), context.dpToPx(7f).toInt())
        background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = context.dpToPx(6f).toInt()
            topMargin = context.dpToPx(6f).toInt()
        }
        setOnClickListener { onClick() }
    }

    fun prepare() {
        queryField.setText(listener?.searchInitialQuery().orEmpty())
        queryField.setSelection(queryField.text?.length ?: 0)
        refreshContacts(currentQuery())
        applyTheme(theme)
    }

    private fun currentQuery(): String = queryField.text?.toString().orEmpty()

    private fun refreshContacts(query: String) {
        resultList.removeAllViews()
        if (!contacts.isEnabled) {
            hintLabel.text = context.getString(R.string.search_contacts_disabled)
            hintLabel.visible()
            return
        }
        val results = contacts.search(query, 12) + contacts.emails(query, 6)
        if (results.isEmpty()) {
            hintLabel.text = context.getString(R.string.search_contacts_hint)
            hintLabel.visible()
            return
        }
        hintLabel.gone()
        results.take(16).forEach { contact ->
            val row = TextView(context).apply {
                text = context.getString(R.string.search_contact_row, contact.name, contact.detail)
                textSize = 14f
                setPadding(context.dpToPx(14f).toInt(), context.dpToPx(10f).toInt(),
                    context.dpToPx(14f).toInt(), context.dpToPx(10f).toInt())
                background = ContextCompat.getDrawable(context, R.drawable.bg_suggestion_ripple)
                setTextColor(theme.keyText)
                setOnClickListener { listener?.onSearchInsert(contact.detail) }
            }
            resultList.addView(
                row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
        }
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        queryField.setTextColor(theme.keyText)
        queryField.setHintTextColor(theme.keySecondaryText)
        queryField.backgroundTintList =
            android.content.res.ColorStateList.valueOf(theme.keyBackground)
        hintLabel.setTextColor(theme.keySecondaryText)
        closeButton.setColorFilter(theme.keySecondaryText)
        for (index in 0 until providerRow.childCount) {
            val chip = providerRow.getChildAt(index) as TextView
            chip.setTextColor(theme.keyText)
            chip.backgroundTintList =
                android.content.res.ColorStateList.valueOf(theme.keyBackground)
        }
        for (index in 0 until resultList.childCount) {
            (resultList.getChildAt(index) as? TextView)?.setTextColor(theme.keyText)
        }
    }

    override val isAcceptingText: Boolean get() = queryFocused

    override fun onTextCommitted(text: String) {
        queryField.append(text)
        queryField.setSelection(queryField.text?.length ?: 0)
    }

    override fun onBackspacePressed() {
        val value = currentQuery()
        if (value.isNotEmpty()) queryField.setText(value.dropLast(1))
        queryField.setSelection(queryField.text?.length ?: 0)
    }

    override fun onEnterPressed() {
        webSearch.search(currentQuery())
    }
}
