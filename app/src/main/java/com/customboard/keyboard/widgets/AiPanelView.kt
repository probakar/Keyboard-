package com.customboard.keyboard.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.ai.AiAction
import com.customboard.keyboard.ai.AiResult
import com.customboard.keyboard.ai.AiWritingAssistant
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.setVisible
import com.customboard.keyboard.utils.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The AI panel: every Gemini and ML Kit powered action, the prompt field, and the result card
 * with insert / replace / copy / retry.
 */
class AiPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), TextInputTarget {

    interface Listener {
        fun onAiInsert(text: String)
        fun onAiReplace(text: String)
        fun onAiCopy(text: String)
        fun onAiPanelClosed()
        fun onAiSettingsRequested()
        fun aiInputText(): String
        fun aiContextText(): String
    }

    var listener: Listener? = null

    private val assistant = AiWritingAssistant(context)
    private val prefs = PreferencesManager.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private val actionRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val optionRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val optionScroll = HorizontalScrollView(context).apply {
        isHorizontalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
    }
    private val promptField = EditText(context)
    private val resultText = TextView(context)
    private val statusText = TextView(context)
    private val progress = ProgressBar(context)
    private val resultCard = LinearLayout(context).apply { orientation = VERTICAL }
    private val buttonRow = LinearLayout(context).apply { orientation = HORIZONTAL }
    private val closeButton = ImageButton(context)
    private val settingsButton = ImageButton(context)
    private val titleLabel = TextView(context)
    private val sendButton = ImageButton(context)

    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var selectedAction: AiAction = AiAction.REWRITE
    private var lastResult: String = ""
    private var promptFocused = false

    init {
        orientation = VERTICAL
        buildHeader()
        buildActions()
        buildOptions()
        buildPrompt()
        buildResult()
        buildFooter()
        showIdle()
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleLabel.apply {
            text = context.getString(R.string.ai_title)
            textSize = 15f
            setPadding(context.dpToPx(14f).toInt(), 0, 0, 0)
        }
        header.addView(titleLabel, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        settingsButton.apply {
            setImageResource(R.drawable.ic_settings)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.ai_settings)
            setOnClickListener { listener?.onAiSettingsRequested() }
        }
        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(context.dpToPx(40f).toInt(), context.dpToPx(40f).toInt())
        )
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    private fun buildActions() {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            addView(
                actionRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        AiAction.entries.forEach { action ->
            actionRow.addView(createActionChip(action))
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(56f).toInt()))
    }

    private fun createActionChip(action: AiAction): View {
        val chip = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(10f).toInt(), context.dpToPx(6f).toInt(),
                context.dpToPx(10f).toInt(), context.dpToPx(6f).toInt())
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
            tag = action.id
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            ).apply {
                marginStart = context.dpToPx(6f).toInt()
                topMargin = context.dpToPx(6f).toInt()
                bottomMargin = context.dpToPx(6f).toInt()
            }
            setOnClickListener { selectAction(action) }
        }
        val icon = android.widget.ImageView(context).apply {
            setImageResource(action.iconRes)
            layoutParams = LinearLayout.LayoutParams(
                context.dpToPx(20f).toInt(), context.dpToPx(20f).toInt()
            )
        }
        val label = TextView(context).apply {
            text = context.getString(action.titleRes)
            textSize = 11f
            gravity = Gravity.CENTER
        }
        chip.addView(icon)
        chip.addView(label)
        return chip
    }

    private fun buildOptions() {
        optionScroll.addView(
            optionRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        addView(optionScroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(38f).toInt()))
        optionScroll.gone()
    }

    private fun buildPrompt() {
        val row = FrameLayout(context)
        promptField.apply {
            hint = context.getString(R.string.ai_prompt_hint)
            setSingleLine(false)
            maxLines = 2
            textSize = 14f
            background = ContextCompat.getDrawable(context, R.drawable.bg_input_field)
            showSoftInputOnFocus = false
            isFocusableInTouchMode = true
            setPadding(context.dpToPx(12f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(44f).toInt(), context.dpToPx(8f).toInt())
            setOnFocusChangeListener { _, hasFocus -> promptFocused = hasFocus }
        }
        row.addView(
            promptField,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = context.dpToPx(10f).toInt()
                marginEnd = context.dpToPx(10f).toInt()
            }
        )
        sendButton.apply {
            setImageResource(R.drawable.ic_send)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.ai_run)
            setOnClickListener { run() }
        }
        row.addView(
            sendButton,
            FrameLayout.LayoutParams(
                context.dpToPx(40f).toInt(), context.dpToPx(40f).toInt(), Gravity.END
            ).apply { marginEnd = context.dpToPx(12f).toInt() }
        )
        addView(
            row,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = context.dpToPx(4f).toInt()
            }
        )
    }

    private fun buildResult() {
        val scroll = ScrollView(context).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        resultCard.apply {
            setPadding(context.dpToPx(12f).toInt(), context.dpToPx(10f).toInt(),
                context.dpToPx(12f).toInt(), context.dpToPx(10f).toInt())
        }
        statusText.apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(10f).toInt(), 0, context.dpToPx(10f).toInt())
        }
        resultText.apply {
            textSize = 15f
            setTextIsSelectable(false)
        }
        progress.apply {
            isIndeterminate = true
            gone()
        }
        resultCard.addView(progress, LayoutParams(context.dpToPx(28f).toInt(), context.dpToPx(28f).toInt()).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        resultCard.addView(statusText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        resultCard.addView(resultText, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        scroll.addView(
            resultCard,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
        )
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun buildFooter() {
        buttonRow.apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpToPx(8f).toInt(), 0, context.dpToPx(8f).toInt(), 0)
        }
        addView(buttonRow, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(46f).toInt()))

        closeButton.apply {
            setImageResource(R.drawable.ic_keyboard)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.cd_back_to_keyboard)
            setOnClickListener {
                job?.cancel()
                listener?.onAiPanelClosed()
            }
        }
        buttonRow.addView(
            closeButton,
            LinearLayout.LayoutParams(context.dpToPx(44f).toInt(), LayoutParams.MATCH_PARENT)
        )
        buttonRow.addView(
            createTextButton(R.string.ai_insert) { listener?.onAiInsert(lastResult) },
            buttonParams()
        )
        buttonRow.addView(
            createTextButton(R.string.ai_replace) { listener?.onAiReplace(lastResult) },
            buttonParams()
        )
        buttonRow.addView(
            createTextButton(R.string.ai_copy) { listener?.onAiCopy(lastResult) },
            buttonParams()
        )
        buttonRow.addView(
            createTextButton(R.string.ai_retry) { run(force = true) },
            buttonParams()
        )
    }

    private fun buttonParams() = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        .apply { marginStart = context.dpToPx(4f).toInt() }

    private fun createTextButton(textRes: Int, onClick: () -> Unit): TextView =
        TextView(context).apply {
            text = context.getString(textRes)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(6f).toInt(), context.dpToPx(8f).toInt(),
                context.dpToPx(6f).toInt(), context.dpToPx(8f).toInt())
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
            setOnClickListener { onClick() }
        }

    // ------------------------------------------------------------------

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        titleLabel.setTextColor(theme.keyText)
        resultText.setTextColor(theme.keyText)
        statusText.setTextColor(theme.keySecondaryText)
        promptField.setTextColor(theme.keyText)
        promptField.setHintTextColor(theme.keySecondaryText)
        promptField.backgroundTintList =
            android.content.res.ColorStateList.valueOf(theme.keyBackground)
        listOf(closeButton, settingsButton).forEach { it.setColorFilter(theme.keySecondaryText) }
        sendButton.setColorFilter(theme.accent)
        progress.indeterminateTintList = android.content.res.ColorStateList.valueOf(theme.accent)
        styleChips(actionRow) { it == selectedAction.id }
        styleChips(optionRow) { it == currentOptionValue() }
        for (index in 1 until buttonRow.childCount) {
            val button = buttonRow.getChildAt(index) as? TextView ?: continue
            button.setTextColor(theme.keyText)
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(theme.keyBackground)
        }
    }

    private fun styleChips(row: LinearLayout, isSelected: (String) -> Boolean) {
        for (index in 0 until row.childCount) {
            val chip = row.getChildAt(index)
            val id = chip.tag as? String ?: continue
            val selected = isSelected(id)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (selected) theme.accent else theme.keyBackground
            )
            val color = if (selected) theme.keyAccentText else theme.keyText
            when (chip) {
                is LinearLayout -> for (child in 0 until chip.childCount) {
                    when (val view = chip.getChildAt(child)) {
                        is TextView -> view.setTextColor(color)
                        is android.widget.ImageView -> view.setColorFilter(color)
                    }
                }

                is TextView -> chip.setTextColor(color)
            }
        }
    }

    fun selectAction(action: AiAction) {
        selectedAction = action
        lastResult = ""
        resultText.text = ""
        buildOptionsFor(action)
        promptField.setVisible(
            action == AiAction.ASK || action == AiAction.COMPOSE || action == AiAction.CUSTOM
        )
        sendButton.setVisible(promptField.visibility == View.VISIBLE)
        showIdle()
        applyTheme(theme)
        if (action != AiAction.ASK && action != AiAction.COMPOSE && action != AiAction.CUSTOM) {
            run()
        }
    }

    private fun buildOptionsFor(action: AiAction) {
        optionRow.removeAllViews()
        val options: List<Pair<String, String>> = when (action) {
            AiAction.TONE, AiAction.REPLY -> context.resources
                .getStringArray(R.array.ai_tone_values)
                .zip(context.resources.getStringArray(R.array.ai_tone_entries))

            AiAction.TRANSLATE -> context.resources
                .getStringArray(R.array.translate_values)
                .zip(context.resources.getStringArray(R.array.translate_entries))

            else -> emptyList()
        }
        optionScroll.setVisible(options.isNotEmpty())
        options.forEach { (value, label) ->
            val chip = TextView(context).apply {
                text = label
                textSize = 12f
                gravity = Gravity.CENTER
                tag = value
                setPadding(context.dpToPx(12f).toInt(), context.dpToPx(5f).toInt(),
                    context.dpToPx(12f).toInt(), context.dpToPx(5f).toInt())
                background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = context.dpToPx(6f).toInt()
                    topMargin = context.dpToPx(4f).toInt()
                }
                setOnClickListener {
                    when (selectedAction) {
                        AiAction.TRANSLATE -> prefs.aiTranslateTarget = value
                        else -> prefs.aiTone = value
                    }
                    applyTheme(this@AiPanelView.theme)
                    run(force = true)
                }
            }
            optionRow.addView(chip)
        }
    }

    private fun currentOptionValue(): String = when (selectedAction) {
        AiAction.TRANSLATE -> prefs.aiTranslateTarget
        else -> prefs.aiTone
    }

    private fun showIdle() {
        progress.gone()
        statusText.visible()
        statusText.text = when {
            !prefs.aiEnabled -> context.getString(R.string.ai_error_disabled)
            !assistant.hasApiKey && selectedAction.isCloud ->
                context.getString(R.string.ai_error_no_key)

            else -> context.getString(R.string.ai_hint_pick_action)
        }
        setButtonsEnabled(false)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        for (index in 1 until buttonRow.childCount) {
            buttonRow.getChildAt(index).isEnabled = enabled
            buttonRow.getChildAt(index).alpha = if (enabled) 1f else 0.45f
        }
    }

    fun run(force: Boolean = false) {
        job?.cancel()
        val promptText = promptField.text?.toString().orEmpty().trim()
        val selection = listener?.aiInputText().orEmpty()
        val conversation = listener?.aiContextText().orEmpty()

        val input = when (selectedAction) {
            AiAction.ASK, AiAction.COMPOSE -> promptText
            AiAction.CUSTOM -> selection.ifBlank { conversation }
            AiAction.REPLY -> conversation
            else -> selection.ifBlank { conversation }
        }
        if (input.isBlank() && selectedAction != AiAction.CUSTOM) {
            statusText.visible()
            statusText.text = context.getString(R.string.ai_error_empty_input)
            return
        }

        progress.visible()
        statusText.visible()
        statusText.text = context.getString(R.string.ai_working)
        resultText.text = ""
        setButtonsEnabled(false)

        val extra = when (selectedAction) {
            AiAction.TRANSLATE -> prefs.aiTranslateTarget
            AiAction.TONE, AiAction.REPLY -> prefs.aiTone
            AiAction.CUSTOM -> promptText
            else -> ""
        }

        job = scope.launch {
            val result = assistant.run(selectedAction, input, extra, conversation)
            progress.gone()
            when (result) {
                is AiResult.Success -> {
                    lastResult = result.text
                    statusText.gone()
                    resultText.text = result.text
                    setButtonsEnabled(true)
                }

                is AiResult.Error -> {
                    lastResult = ""
                    statusText.visible()
                    statusText.text = result.message
                    setButtonsEnabled(false)
                    if (result.needsKey) listener?.onAiSettingsRequested()
                }
            }
        }
    }

    fun prepare(action: AiAction = AiAction.REWRITE) {
        promptField.setText("")
        selectAction(action)
    }

    fun cancel() {
        job?.cancel()
    }

    override val isAcceptingText: Boolean
        get() = promptFocused && promptField.visibility == View.VISIBLE

    override fun onTextCommitted(text: String) {
        if (!isAcceptingText) return
        promptField.append(text)
        promptField.setSelection(promptField.text?.length ?: 0)
    }

    override fun onBackspacePressed() {
        if (!isAcceptingText) return
        val value = promptField.text?.toString().orEmpty()
        if (value.isNotEmpty()) promptField.setText(value.dropLast(1))
        promptField.setSelection(promptField.text?.length ?: 0)
    }

    override fun onEnterPressed() {
        if (!isAcceptingText) return
        run()
    }
}
