package com.customboard.keyboard.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.handwriting.DigitalInkHandwritingRecognizer
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.utils.dpToPx
import com.google.mlkit.vision.digitalink.recognition.Ink

/** Drawing pad and language selector for offline-after-download handwriting input. */
class HandwritingPanelView @JvmOverloads constructor(
    context: Context,
    attrs: android.util.AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    data class LanguageOption(val code: String, val label: String)

    interface Listener {
        fun onHandwritingCommitted(text: String)
        fun onHandwritingAlternative(text: String)
        fun onHandwritingSpace()
        fun onHandwritingDelete()
        fun onHandwritingClear()
        fun onHandwritingClosed()
        fun handwritingPreContext(): String
    }

    var listener: Listener? = null

    private val recognizer = DigitalInkHandwritingRecognizer()
    private val handler = Handler(Looper.getMainLooper())
    private val canvas = HandwritingCanvasView(context)
    private val languageButton = TextView(context)
    private val statusLabel = TextView(context)
    private val candidateStrip = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    private var languages: List<LanguageOption> = DEFAULT_LANGUAGES
    private var languageCode: String = "en_US"
    private var theme: ThemeColors = ThemeManager.getInstance(context).current
    private var canvasRevision = 0
    private var attached = false

    private val recognizeRunnable = Runnable { recognizeCanvas() }

    init {
        orientation = VERTICAL
        setPadding(context.dpToPx(7f).toInt(), 0, context.dpToPx(7f).toInt(), 0)
        buildHeader()
        buildStatus()
        buildCanvas()
        buildCandidates()
        buildControls()
    }

    fun prepare(options: List<LanguageOption>, initialLanguage: String) {
        languages = options.ifEmpty { DEFAULT_LANGUAGES }
        languageCode = languages.firstOrNull { it.code == initialLanguage }?.code
            ?: languages.first().code
        canvasRevision++
        handler.removeCallbacks(recognizeRunnable)
        recognizer.cancel(languageCode)
        canvas.clear(notify = false)
        candidateStrip.removeAllViews()
        candidateStrip.visibility = View.GONE
        updateLanguageButton()
        statusLabel.setText(R.string.handwriting_status_idle)
        applyTheme(theme)
    }

    fun close() {
        handler.removeCallbacks(recognizeRunnable)
        recognizer.close()
    }

    fun applyTheme(theme: ThemeColors) {
        this.theme = theme
        setBackgroundColor(theme.background)
        statusLabel.setTextColor(theme.keySecondaryText)
        languageButton.setTextColor(theme.accent)
        canvas.setTheme(theme)
        for (index in 0 until candidateStrip.childCount) {
            (candidateStrip.getChildAt(index) as? TextView)?.setTextColor(theme.keyText)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attached = true
    }

    override fun onDetachedFromWindow() {
        attached = false
        handler.removeCallbacks(recognizeRunnable)
        recognizer.cancel(languageCode)
        super.onDetachedFromWindow()
    }

    private fun buildHeader() {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(context).apply {
            setText(R.string.handwriting_title)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(context.dpToPx(6f).toInt(), 0, context.dpToPx(5f).toInt(), 0)
        }
        header.addView(title, LayoutParams(0, context.dpToPx(40f).toInt(), 1f))

        languageButton.apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(context.dpToPx(9f).toInt(), 0, context.dpToPx(9f).toInt(), 0)
            background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
            contentDescription = context.getString(R.string.handwriting_choose_language)
            setOnClickListener { showLanguageMenu() }
        }
        header.addView(languageButton, LayoutParams(LayoutParams.WRAP_CONTENT, context.dpToPx(34f).toInt()))

        header.addView(
            iconButton(R.drawable.ic_delete_sweep, R.string.handwriting_clear) { clearCanvas() },
            LayoutParams(context.dpToPx(40f).toInt(), context.dpToPx(40f).toInt())
        )
        header.addView(
            iconButton(R.drawable.ic_keyboard, R.string.cd_back_to_keyboard) {
                listener?.onHandwritingClosed()
            },
            LayoutParams(context.dpToPx(40f).toInt(), context.dpToPx(40f).toInt())
        )
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    private fun buildStatus() {
        statusLabel.apply {
            textSize = 10.5f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(context.dpToPx(7f).toInt(), 0, context.dpToPx(7f).toInt(), 0)
            setText(R.string.handwriting_status_idle)
        }
        addView(statusLabel, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(22f).toInt()))
    }

    private fun buildCanvas() {
        canvas.background = ContextCompat.getDrawable(context, R.drawable.bg_card)
        canvas.onInkChanged = {
            canvasRevision++
            recognizer.cancel(languageCode)
            candidateStrip.removeAllViews()
            candidateStrip.visibility = View.GONE
            statusLabel.setText(R.string.handwriting_status_waiting)
            handler.removeCallbacks(recognizeRunnable)
            handler.postDelayed(recognizeRunnable, RECOGNITION_DELAY_MS)
        }
        canvas.minimumHeight = context.dpToPx(84f).toInt()
        addView(canvas, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
            val margin = context.dpToPx(3f).toInt()
            setMargins(margin, margin, margin, margin)
        })
    }

    private fun buildCandidates() {
        val scroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            visibility = View.GONE
            addView(candidateStrip, HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT
            ))
        }
        addView(scroll, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(32f).toInt()))
    }

    private fun buildControls() {
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val delete = iconButton(R.drawable.ic_backspace, R.string.handwriting_backspace) {
            listener?.onHandwritingDelete()
            candidateStrip.removeAllViews()
            candidateStrip.visibility = View.GONE
            statusLabel.setText(R.string.handwriting_status_idle)
        }
        controls.addView(delete, LayoutParams(context.dpToPx(48f).toInt(), context.dpToPx(38f).toInt()))

        val space = TextView(context).apply {
            setText(R.string.handwriting_space)
            textSize = 12f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(R.string.handwriting_space)
            setOnClickListener {
                listener?.onHandwritingSpace()
                candidateStrip.removeAllViews()
                candidateStrip.visibility = View.GONE
                statusLabel.setText(R.string.handwriting_status_idle)
            }
        }
        controls.addView(space, LayoutParams(0, context.dpToPx(38f).toInt(), 1f))

        val clear = TextView(context).apply {
            setText(R.string.handwriting_clear)
            textSize = 11f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            setOnClickListener { clearCanvas() }
        }
        controls.addView(clear, LayoutParams(context.dpToPx(64f).toInt(), context.dpToPx(38f).toInt()))
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, context.dpToPx(40f).toInt()))
    }

    private fun iconButton(iconRes: Int, descriptionRes: Int, click: () -> Unit) =
        ImageButton(context).apply {
            setImageResource(iconRes)
            background = ContextCompat.getDrawable(context, R.drawable.bg_toolbar_ripple)
            contentDescription = context.getString(descriptionRes)
            setOnClickListener { click() }
        }

    private fun showLanguageMenu() {
        val menu = PopupMenu(context, languageButton)
        languages.forEachIndexed { index, option -> menu.menu.add(0, index, index, option.label) }
        menu.setOnMenuItemClickListener { item ->
            val option = languages.getOrNull(item.itemId) ?: return@setOnMenuItemClickListener false
            languageCode = option.code
            canvasRevision++
            recognizer.cancel(languageCode)
            canvas.clear(notify = false)
            candidateStrip.removeAllViews()
            candidateStrip.visibility = View.GONE
            updateLanguageButton()
            statusLabel.text = context.getString(R.string.handwriting_language_selected, option.label)
            true
        }
        menu.show()
    }

    private fun updateLanguageButton() {
        val label = languages.firstOrNull { it.code == languageCode }?.label.orEmpty()
        languageButton.text = "$label ▾"
    }

    private fun recognizeCanvas() {
        if (!attached || !canvas.hasInk()) return
        val revision = canvasRevision
        val ink = canvas.buildInk() ?: return
        val width = canvas.width.toFloat().coerceAtLeast(1f)
        val height = canvas.height.toFloat().coerceAtLeast(1f)
        statusLabel.setText(R.string.handwriting_status_checking)
        recognizer.recognize(
            languageCode = languageCode,
            ink = ink,
            width = width,
            height = height,
            preContext = listener?.handwritingPreContext().orEmpty(),
            onStatus = { message ->
                if (attached && revision == canvasRevision) statusLabel.text = message
            },
            onCandidates = { candidates ->
                if (!attached || revision != canvasRevision) return@recognize
                if (candidates.isEmpty()) {
                    statusLabel.setText(R.string.handwriting_status_not_recognized)
                    return@recognize
                }
                val best = candidates.first()
                listener?.onHandwritingCommitted(best)
                statusLabel.text = context.getString(R.string.handwriting_status_typed, best)
                showAlternativeCandidates(candidates.drop(1))
                canvas.clear(notify = false)
            }
        )
    }

    private fun showAlternativeCandidates(candidates: List<String>) {
        candidateStrip.removeAllViews()
        candidates.forEach { candidate ->
            val chip = TextView(context).apply {
                text = candidate
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(context.dpToPx(12f).toInt(), 0, context.dpToPx(12f).toInt(), 0)
                background = ContextCompat.getDrawable(context, R.drawable.bg_chip)
                setTextColor(theme.keyText)
                setOnClickListener {
                    listener?.onHandwritingAlternative(candidate)
                    statusLabel.text = context.getString(R.string.handwriting_status_typed, candidate)
                }
            }
            candidateStrip.addView(chip, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))
        }
        candidateStrip.visibility = if (candidates.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun clearCanvas() {
        canvasRevision++
        handler.removeCallbacks(recognizeRunnable)
        recognizer.cancel(languageCode)
        canvas.clear(notify = false)
        candidateStrip.removeAllViews()
        candidateStrip.visibility = View.GONE
        statusLabel.setText(R.string.handwriting_status_idle)
        listener?.onHandwritingClear()
    }

    companion object {
        private const val RECOGNITION_DELAY_MS = 650L
        val DEFAULT_LANGUAGES = listOf(
            LanguageOption("en_US", "English (US)"),
            LanguageOption("en_GB", "English (UK)"),
            LanguageOption("ur", "اردو · Pakistan"),
            LanguageOption("hi", "हिन्दी"),
            LanguageOption("ar", "العربية"),
            LanguageOption("fr", "Français"),
            LanguageOption("de", "Deutsch"),
            LanguageOption("es", "Español")
        )
    }
}
