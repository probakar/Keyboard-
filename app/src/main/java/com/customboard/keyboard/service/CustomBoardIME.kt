package com.customboard.keyboard.service

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.content.FileProvider
import com.customboard.keyboard.R
import com.customboard.keyboard.accessibility.AccessibilityHelper
import com.customboard.keyboard.ai.AiAction
import com.customboard.keyboard.ai.AiWritingAssistant
import com.customboard.keyboard.autocorrect.GrammarChecker
import com.customboard.keyboard.autocorrect.Suggestion
import com.customboard.keyboard.clipboard.ClipboardManager
import com.customboard.keyboard.databinding.KeyboardViewBinding
import com.customboard.keyboard.emoji.EmojiSearchEngine
import com.customboard.keyboard.emoji.GifSearchManager
import com.customboard.keyboard.gesture.GestureDetector
import com.customboard.keyboard.gesture.GestureInfo
import com.customboard.keyboard.gesture.SpacebarGestureHandler
import com.customboard.keyboard.gesture.SwipeDirection
import com.customboard.keyboard.gesture.SwipeGestureHandler
import com.customboard.keyboard.keyboard.FloatingKeyboardManager
import com.customboard.keyboard.keyboard.GestureTypingEngine
import com.customboard.keyboard.keyboard.KeyPopupManager
import com.customboard.keyboard.keyboard.KeySoundManager
import com.customboard.keyboard.keyboard.KeyboardLayoutManager
import com.customboard.keyboard.keyboard.NumberRowManager
import com.customboard.keyboard.keyboard.OneHandedModeManager
import com.customboard.keyboard.keyboard.SplitKeyboardManager
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.keyboard.model.KeyboardMode
import com.customboard.keyboard.keyboard.model.ShiftState
import com.customboard.keyboard.privacy.IncognitoManager
import com.customboard.keyboard.search.ContactsSearchManager
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.settings.SettingsActivity
import com.customboard.keyboard.textprocessing.MathCalculator
import com.customboard.keyboard.theme.ThemeColors
import com.customboard.keyboard.theme.ThemeManager
import com.customboard.keyboard.theme.ThemePresets
import com.customboard.keyboard.toolbar.ToolbarItem
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.DeviceUtils
import com.customboard.keyboard.utils.KeyCodes
import com.customboard.keyboard.utils.KeyboardUtils
import com.customboard.keyboard.utils.dpToPx
import com.customboard.keyboard.utils.gone
import com.customboard.keyboard.utils.setVisible
import com.customboard.keyboard.utils.visible
import com.customboard.keyboard.voice.VoiceCommandProcessor
import com.customboard.keyboard.voice.VoiceInputManager
import com.customboard.keyboard.widgets.AiPanelView
import com.customboard.keyboard.widgets.CandidateView
import com.customboard.keyboard.widgets.ClipboardPanelView
import com.customboard.keyboard.widgets.CursorControlPanelView
import com.customboard.keyboard.widgets.EmojiPanelView
import com.customboard.keyboard.widgets.KeyboardView
import com.customboard.keyboard.widgets.MediaPanelView
import com.customboard.keyboard.widgets.ResizePanelView
import com.customboard.keyboard.widgets.SearchPanelView
import com.customboard.keyboard.widgets.SuggestionsPanelView
import com.customboard.keyboard.widgets.TextInputTarget
import com.customboard.keyboard.widgets.TextToolsPanelView
import com.customboard.keyboard.widgets.ToolbarView
import com.customboard.keyboard.widgets.VoiceInputView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * CustomBoard's input method service - the heart of the keyboard.
 *
 * It owns the input view, routes every key, gesture and panel action, and keeps the typing
 * engine, suggestion strip, themes and the AI assistant in sync.
 */
class CustomBoardIME : InputMethodService(),
    KeyboardView.KeyboardListener,
    CandidateView.Listener,
    ToolbarView.Listener,
    EmojiPanelView.Listener,
    ClipboardPanelView.Listener,
    AiPanelView.Listener,
    MediaPanelView.Listener,
    TextToolsPanelView.Listener,
    SearchPanelView.Listener,
    SuggestionsPanelView.Listener,
    ResizePanelView.Listener,
    VoiceInputView.Listener,
    VoiceInputManager.Listener {

    private lateinit var binding: KeyboardViewBinding

    private val prefs by lazy { PreferencesManager.getInstance(this) }
    private val themeManager by lazy { ThemeManager.getInstance(this) }
    private val layoutManager by lazy { KeyboardLayoutManager(this) }
    private val soundManager by lazy { KeySoundManager(this) }
    private val popupManager by lazy { KeyPopupManager(this) }
    private val numberRowManager by lazy { NumberRowManager(this) }
    private val oneHandedManager by lazy { OneHandedModeManager(this) }
    private val floatingManager by lazy { FloatingKeyboardManager(this) }
    private val splitManager by lazy { SplitKeyboardManager(this) }
    private val clipboardManager by lazy { ClipboardManager.getInstance(this) }
    private val incognitoManager by lazy { IncognitoManager(this) }
    private val accessibility by lazy { AccessibilityHelper(this) }
    private val gestureEngine by lazy { GestureTypingEngine(this) }
    private val emojiSearch by lazy { EmojiSearchEngine(this) }
    private val contactsManager by lazy { ContactsSearchManager(this) }
    private val assistant by lazy { AiWritingAssistant(this) }
    private val gifManager by lazy { GifSearchManager(this) }
    private val voiceManager by lazy { VoiceInputManager(this) }
    private val swipeHandler by lazy { SwipeGestureHandler(this) }
    private val spacebarHandler by lazy { SpacebarGestureHandler(this) }
    private val gestureDetector by lazy {
        GestureDetector(dpToPx(Constants.GESTURE_MIN_DISTANCE_DP))
    }

    private val inputLogic by lazy { InputLogic(this) { currentInputConnection } }
    private val cursorController by lazy { CursorController { currentInputConnection } }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var suggestionJob: Job? = null

    private var activePanel: View? = null
    private var activeTextTarget: TextInputTarget? = null
    private var currentEditorInfo: EditorInfo? = null
    private var theme: ThemeColors = ThemePresets.LIGHT

    // Panels are created lazily, the first time they are needed.
    private var emojiPanel: EmojiPanelView? = null
    private var clipboardPanel: ClipboardPanelView? = null
    private var aiPanel: AiPanelView? = null
    private var mediaPanel: MediaPanelView? = null
    private var textToolsPanel: TextToolsPanelView? = null
    private var cursorPanel: CursorControlPanelView? = null
    private var searchPanel: SearchPanelView? = null
    private var suggestionsPanel: SuggestionsPanelView? = null
    private var resizePanel: ResizePanelView? = null
    private var voicePanel: VoiceInputView? = null

    private val preferenceListener =
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            handler.post { applyAllPreferences() }
        }

    // ------------------------------------------------------------------
    //  Lifecycle
    // ------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        themeManager.invalidate()
        theme = themeManager.current
        prefs.registerListener(preferenceListener)
        clipboardManager.startMonitoring()
        inputLogic.engine.ensureLoaded()
    }

    override fun onCreateInputView(): View {
        binding = KeyboardViewBinding.inflate(layoutInflater)
        binding.keyboardView.listener = this
        binding.candidateView.listener = this
        binding.toolbarView.listener = this
        floatingManager.attachDragHandle(binding.dragHandle, binding.keyboardContainer)
        applyAllPreferences()
        return binding.root
    }

    override fun onStartInput(info: EditorInfo?, restarting: Boolean) {
        super.onStartInput(info, restarting)
        currentEditorInfo = info
        inputLogic.onStartInput(info)
        incognitoManager.onStartInput(info)
        layoutManager.refreshLanguageFromPrefs()
        layoutManager.adaptToEditor(info)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        currentEditorInfo = info
        if (!::binding.isInitialized) return
        hidePanel()
        themeManager.invalidate()
        theme = themeManager.current
        layoutManager.adaptToEditor(info)
        if (prefs.rememberLayoutPerApp) {
            info?.packageName?.let { layoutManager.restoreLayoutForApp(it) }
        }
        applyAllPreferences()
        updateShiftState(auto = true)
        refreshSuggestions()
        clipboardManager.captureCurrentClip()
        maybeSuggestSmartReply()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        popupManager.dismissAll()
        voiceManager.cancel()
        hidePanel()
        if (::binding.isInitialized) binding.keyboardView.cancelAllInput()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        inputLogic.onFinishInput()
        currentEditorInfo?.packageName?.let { packageName ->
            if (prefs.rememberLayoutPerApp) layoutManager.rememberLayoutForApp(packageName)
        }
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(
            oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd
        )
        cursorController.onUpdateSelection(newSelStart, newSelEnd)
        if (candidatesStart == -1 && candidatesEnd == -1) {
            inputLogic.finishComposing()
        }
        updateShiftState(auto = true)
        refreshSuggestions()
    }

    override fun onDestroy() {
        prefs.unregisterListener(preferenceListener)
        clipboardManager.stopMonitoring()
        voiceManager.release()
        assistant.release()
        popupManager.dismissAll()
        inputLogic.engine.persist()
        super.onDestroy()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && activePanel != null) {
            hidePanel()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ------------------------------------------------------------------
    //  Configuration
    // ------------------------------------------------------------------

    private fun applyAllPreferences() {
        if (!::binding.isInitialized) return
        themeManager.invalidate()
        theme = themeManager.current

        val layout = layoutManager.currentLayout()
        binding.keyboardView.setTheme(theme)
        binding.keyboardView.setKeyboardLayout(layout)
        binding.keyboardView.setSplitEnabled(splitManager.isEnabled)
        binding.keyboardView.setIncognito(incognitoManager.isActive)
        binding.keyboardView.setSpaceBarLabel(layoutManager.spaceBarLabel())
        binding.keyboardView.setEnterLabel(KeyboardUtils.enterKeyLabel(this, currentEditorInfo))
        binding.keyboardView.setShiftState(layoutManager.shiftState)

        binding.root.setBackgroundColor(theme.background)
        binding.keyboardContainer.setBackgroundColor(theme.background)
        binding.candidateView.applyTheme(theme)
        binding.toolbarView.refresh()
        binding.toolbarView.applyTheme(theme)
        binding.toolbarView.setVisible(prefs.toolbarEnabled)
        binding.candidateView.setVisible(inputLogic.isSuggestionAllowed())
        binding.toolbarView.setActive("incognito", incognitoManager.isActive)
        binding.toolbarView.setActive("one_handed", oneHandedManager.side != OneHandedModeManager.Side.OFF)
        binding.toolbarView.setActive("floating", floatingManager.isEnabled)
        binding.toolbarView.setActive("split", splitManager.isEnabled)
        binding.toolbarView.setActive("number_row", numberRowManager.isEnabled)

        popupManager.setTheme(theme)
        updateKeyboardMetrics()
        applyPanelTheme()
    }

    private fun updateKeyboardMetrics() {
        val height = DeviceUtils.defaultKeyboardHeightPx(this) *
            prefs.keyboardHeightPercent / 100
        val params = binding.contentFrame.layoutParams
        params.height = height
        binding.contentFrame.layoutParams = params

        val padding = dpToPx(prefs.bottomPaddingDp.toFloat()).toInt()
        binding.bottomPadding.layoutParams =
            binding.bottomPadding.layoutParams.apply { this.height = padding }

        oneHandedManager.apply(binding.keyboardContainer)
        floatingManager.apply(binding.keyboardContainer)
        binding.dragHandle.setVisible(floatingManager.isEnabled)
    }

    private fun applyPanelTheme() {
        emojiPanel?.applyTheme(theme)
        clipboardPanel?.applyTheme(theme)
        aiPanel?.applyTheme(theme)
        mediaPanel?.applyTheme(theme)
        textToolsPanel?.applyTheme(theme)
        cursorPanel?.applyTheme(theme)
        searchPanel?.applyTheme(theme)
        suggestionsPanel?.applyTheme(theme)
        resizePanel?.applyTheme(theme)
        voicePanel?.applyTheme(theme)
    }

    private fun reloadLayout() {
        if (!::binding.isInitialized) return
        binding.keyboardView.setKeyboardLayout(layoutManager.currentLayout())
        binding.keyboardView.setSpaceBarLabel(layoutManager.spaceBarLabel())
        binding.keyboardView.setShiftState(layoutManager.shiftState)
    }

    private fun updateShiftState(auto: Boolean) {
        if (auto && inputLogic.composing.isEmpty()) {
            val shouldCapitalize = inputLogic.shouldAutoCapitalize()
            layoutManager.autoCapitalize(shouldCapitalize)
        }
        if (::binding.isInitialized) {
            binding.keyboardView.setShiftState(layoutManager.shiftState)
        }
    }

    // ------------------------------------------------------------------
    //  Suggestions
    // ------------------------------------------------------------------

    private fun refreshSuggestions() {
        if (!::binding.isInitialized) return
        if (!inputLogic.isSuggestionAllowed()) {
            binding.candidateView.clear()
            binding.candidateView.gone()
            return
        }
        binding.candidateView.visible()
        suggestionJob?.cancel()
        suggestionJob = scope.launch {
            val composing = inputLogic.composing
            val previous = inputLogic.previousWord()

            // Inline calculator ("12*8+4" -> 100) wins over word suggestions.
            if (prefs.calculatorMode && composing.isEmpty()) {
                val expression = inputLogic.textBeforeCursor(60).takeLastWhile { it != '\n' }
                val result = withContext(Dispatchers.Default) { MathCalculator.evaluate(expression) }
                if (result != null) {
                    binding.candidateView.setChips(
                        listOf(getString(R.string.calculator_result, result)),
                        Suggestion.Source.AI
                    )
                    return@launch
                }
            }

            val words = withContext(Dispatchers.Default) {
                inputLogic.suggestions().toMutableList()
            }

            if (composing.length >= 2) {
                contactsManager.suggestNames(composing).forEach { name ->
                    if (words.none { it.word.equals(name, ignoreCase = true) }) {
                        words.add(1.coerceAtMost(words.size), Suggestion(name, 0.0, Suggestion.Source.CONTACT))
                    }
                }
                emojiSearch.suggestForWord(composing).firstOrNull()?.let { emoji ->
                    words.add(Suggestion(emoji, 0.0, Suggestion.Source.AI))
                }
            }

            if (words.isEmpty() && composing.isEmpty() && prefs.clipboardSuggest) {
                clipboardManager.latestClip?.takeIf { it.length <= 140 }?.let { clip ->
                    binding.candidateView.setChips(
                        listOf(clip), Suggestion.Source.CLIPBOARD, R.drawable.ic_clipboard
                    )
                    return@launch
                }
            }

            if (prefs.grammarCheck && composing.isEmpty()) {
                val issue = withContext(Dispatchers.Default) {
                    GrammarChecker.check(inputLogic.textBeforeCursor(120)).firstOrNull()
                }
                if (issue != null && words.isEmpty()) {
                    binding.candidateView.setChips(
                        listOf(issue.replacement), Suggestion.Source.CORRECTION
                    )
                    return@launch
                }
            }

            binding.candidateView.setSuggestions(words)
        }
    }

    private fun maybeSuggestSmartReply() {
        if (!prefs.aiSmartReply || incognitoManager.isActive) return
        val draft = inputLogic.textBeforeCursor(400)
        if (draft.isNotBlank()) return
        scope.launch {
            val context = withContext(Dispatchers.Default) { inputLogic.allText(400) }
            if (context.isBlank()) return@launch
            val replies = assistant.quickReplies(context)
            if (replies.isNotEmpty() && inputLogic.composing.isEmpty()) {
                binding.candidateView.setChips(replies, Suggestion.Source.AI, R.drawable.ic_ai)
            }
        }
    }

    // ------------------------------------------------------------------
    //  KeyboardView.KeyboardListener
    // ------------------------------------------------------------------

    override fun onKeyDown(key: Key) {
        soundManager.playFor(key, binding.keyboardView)
        if (prefs.keyPreview && key.type == KeyType.CHARACTER) {
            binding.keyboardView.placements()
                .firstOrNull { it.key === key }
                ?.let { placement ->
                    popupManager.showPreview(
                        binding.keyboardView, key, placement.rect,
                        layoutManager.shiftState.isUppercase
                    )
                }
        }
        if (accessibility.isScreenReaderOn) {
            accessibility.announceKey(
                binding.keyboardView, key, layoutManager.shiftState.isUppercase
            )
        }
    }

    override fun onKeyUp(key: Key) {
        popupManager.hidePreview()
        handleKey(key)
    }

    override fun onKeyRepeat(key: Key) {
        when (key.code) {
            KeyCodes.DELETE -> {
                inputLogic.onDelete()
                refreshSuggestions()
            }

            KeyCodes.CURSOR_LEFT -> cursorController.move(-1)
            KeyCodes.CURSOR_RIGHT -> cursorController.move(1)
            KeyCodes.CURSOR_UP -> cursorController.moveVertical(-1)
            KeyCodes.CURSOR_DOWN -> cursorController.moveVertical(1)
            else -> handleKey(key)
        }
    }

    override fun onKeyLongPress(key: Key): Boolean {
        val placement = binding.keyboardView.placements().firstOrNull { it.key === key }
        if (key.popupKeys.isNotEmpty() && placement != null) {
            val shown = popupManager.showAlternates(
                binding.keyboardView, key, placement.rect, layoutManager.shiftState.isUppercase
            )
            if (shown) {
                soundManager.gestureFeedback(binding.keyboardView)
                return true
            }
        }
        return when (key.code) {
            KeyCodes.SPACE -> {
                showImePicker()
                true
            }

            KeyCodes.DELETE -> {
                inputLogic.onDelete(wholeWord = true)
                refreshSuggestions()
                true
            }

            KeyCodes.ENTER -> {
                inputLogic.commitText("\n")
                true
            }

            KeyCodes.EMOJI -> {
                openPanel(PanelType.MEDIA)
                true
            }

            KeyCodes.SHIFT -> {
                layoutManager.setShift(ShiftState.LOCKED)
                binding.keyboardView.setShiftState(ShiftState.LOCKED)
                true
            }

            KeyCodes.LANGUAGE -> {
                showImePicker()
                true
            }

            KeyCodes.SETTINGS -> {
                openSettings()
                true
            }

            else -> false
        }
    }

    override fun onPopupCharSelected(text: String) {
        commitTextOrRoute(text)
        layoutManager.consumeShift()
        updateShiftState(auto = false)
        refreshSuggestions()
    }

    override fun onPopupMove(x: Float, y: Float) {
        popupManager.updateAlternates(x, y)
    }

    override fun onPopupRelease() {
        val selected = popupManager.commitAlternates()
        if (!selected.isNullOrEmpty()) onPopupCharSelected(selected)
    }

    override fun onGestureTypingStarted() {
        popupManager.dismissAll()
        soundManager.gestureFeedback(binding.keyboardView)
    }

    override fun onGestureTypingFinished(
        points: List<PointF>,
        placements: List<KeyboardView.KeyPlacement>
    ) {
        if (!prefs.gestureTyping || points.size < 3) return
        // Reject accidental drags that never really left the first key.
        gestureDetector.begin(points.first().x, points.first().y)
        points.drop(1).forEach { gestureDetector.update(it.x, it.y) }
        val travelled = gestureDetector.pathLength()
        gestureDetector.reset()
        if (travelled < dpToPx(Constants.GESTURE_MIN_DISTANCE_DP) * 2f) return
        val letters = HashMap<Char, PointF>()
        placements.forEach { placement ->
            val key = placement.key
            if (key.isLetter && key.label.length == 1) {
                letters[key.label.lowercase()[0]] = PointF(placement.centerX, placement.centerY)
            }
        }
        if (letters.isEmpty()) return
        val previous = inputLogic.previousWord()
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                gestureEngine.recognize(points, letters, previous)
            }
            val best = result.best ?: return@launch
            val word = if (layoutManager.shiftState.isUppercase) {
                best.replaceFirstChar { it.uppercase() }
            } else {
                best
            }
            val needsSpace = inputLogic.textBeforeCursor(1).let {
                it.isNotEmpty() && !it.last().isWhitespace()
            }
            inputLogic.commitText(if (needsSpace) " $word" else word)
            inputLogic.engine.learn(word, previous)
            layoutManager.consumeShift()
            updateShiftState(auto = true)
            binding.candidateView.setSuggestions(
                result.words.mapIndexed { index, candidate ->
                    Suggestion(candidate, 100.0 - index, Suggestion.Source.GESTURE)
                }
            )
        }
    }

    override fun onSpaceSlide(deltaPx: Float) {
        if (!spacebarHandler.isActive) {
            if (!spacebarHandler.begin()) return
            cursorController.beginSlide()
            inputLogic.finishComposing()
        }
        val delta = spacebarHandler.update(deltaPx)
        if (delta != 0) cursorController.slideTo(spacebarHandler.steps)
    }

    override fun onSpaceSlideFinished() {
        if (!spacebarHandler.isActive) return
        spacebarHandler.end()
        cursorController.endSlide()
        refreshSuggestions()
    }

    override fun onSwipeDown() {
        runGesture(SwipeDirection.DOWN)
    }

    override fun onSwipeUpOnKey(key: Key) {
        val hint = key.hint ?: key.popupKeys.firstOrNull()
        val info = gestureInfo(SwipeDirection.UP)
        if (swipeHandler.insertsHint(info, hint) && !hint.isNullOrEmpty()) {
            commitTextOrRoute(hint)
            refreshSuggestions()
            return
        }
        val action = swipeHandler.resolve(info, hint)
        if (action != KeyCodes.NONE) handleFunctionKey(action)
    }

    override fun onDeleteSwipe() {
        runGesture(SwipeDirection.LEFT)
    }

    private fun gestureInfo(direction: SwipeDirection, pointers: Int = 1): GestureInfo =
        GestureInfo(direction, dpToPx(Constants.SWIPE_THRESHOLD_DP), 120L, pointers)

    private fun runGesture(direction: SwipeDirection) {
        val action = swipeHandler.resolve(gestureInfo(direction))
        if (action != KeyCodes.NONE) handleFunctionKey(action)
    }

    // ------------------------------------------------------------------
    //  Key dispatch
    // ------------------------------------------------------------------

    private fun handleKey(key: Key) {
        val code = key.code
        when {
            code == KeyCodes.NONE -> Unit
            code > 0 || key.output != null -> {
                val raw = key.outputText
                val text = if (layoutManager.shiftState.isUppercase) raw.uppercase() else raw
                commitTextOrRoute(text)
                layoutManager.consumeShift()
                updateShiftState(auto = false)
                refreshSuggestions()
            }

            else -> handleFunctionKey(code)
        }
    }

    private fun handleFunctionKey(code: Int) {
        when (code) {
            KeyCodes.SHIFT -> {
                layoutManager.onShiftTap()
                binding.keyboardView.setShiftState(layoutManager.shiftState)
            }

            KeyCodes.SHIFT_LOCK -> {
                layoutManager.setShift(ShiftState.LOCKED)
                binding.keyboardView.setShiftState(ShiftState.LOCKED)
            }

            KeyCodes.DELETE -> {
                if (routeBackspace()) return
                inputLogic.onDelete()
                updateShiftState(auto = true)
                refreshSuggestions()
            }

            KeyCodes.DELETE_WORD -> {
                inputLogic.onDelete(wholeWord = true)
                refreshSuggestions()
            }

            KeyCodes.ENTER -> {
                activeTextTarget?.let {
                    if (it.isAcceptingText) {
                        it.onEnterPressed()
                        return
                    }
                }
                inputLogic.onEnter(currentEditorInfo)
                updateShiftState(auto = true)
                refreshSuggestions()
            }

            KeyCodes.SPACE -> {
                if (routeText(" ")) return
                inputLogic.onSpace()
                layoutManager.consumeShift()
                updateShiftState(auto = true)
                refreshSuggestions()
            }

            KeyCodes.MODE_SYMBOLS -> switchMode(KeyboardMode.SYMBOLS)
            KeyCodes.MODE_SYMBOLS_2 -> switchMode(KeyboardMode.SYMBOLS_2)
            KeyCodes.MODE_LETTERS -> switchMode(KeyboardMode.LETTERS)
            KeyCodes.MODE_NUMPAD -> switchMode(KeyboardMode.NUMPAD)
            KeyCodes.MODE_PHONE -> switchMode(KeyboardMode.PHONE)

            KeyCodes.MODE_NUMBER_ROW -> {
                numberRowManager.toggle()
                reloadLayout()
                binding.toolbarView.setActive("number_row", numberRowManager.isEnabled)
            }

            KeyCodes.LANGUAGE -> {
                val language = layoutManager.switchToNextLanguage()
                reloadLayout()
                showToast(getString(R.string.language_switched, language))
            }

            KeyCodes.EMOJI -> openPanel(PanelType.EMOJI)
            KeyCodes.CLIPBOARD -> openPanel(PanelType.CLIPBOARD)
            KeyCodes.AI_TOOLS -> openPanel(PanelType.AI)
            KeyCodes.TEXT_TOOLS -> openPanel(PanelType.TEXT_TOOLS)
            KeyCodes.GIF -> openPanel(PanelType.MEDIA)
            KeyCodes.STICKER -> openPanel(PanelType.MEDIA)
            KeyCodes.SEARCH -> openPanel(PanelType.SEARCH)
            KeyCodes.CONTACTS -> openPanel(PanelType.SEARCH)
            KeyCodes.RESIZE -> openPanel(PanelType.RESIZE)
            KeyCodes.SELECT_MODE -> openPanel(PanelType.CURSOR)
            KeyCodes.VOICE -> startVoiceInput()
            KeyCodes.SETTINGS -> openSettings()
            KeyCodes.THEME -> openSettings(SettingsActivity.SECTION_THEME)

            KeyCodes.TRANSLATE -> {
                openPanel(PanelType.AI)
                aiPanel?.prepare(AiAction.TRANSLATE)
            }

            KeyCodes.COMPOSE -> {
                openPanel(PanelType.AI)
                aiPanel?.prepare(AiAction.COMPOSE)
            }

            KeyCodes.HIDE_KEYBOARD, KeyCodes.BACK_TO_KEYBOARD -> {
                if (activePanel != null) hidePanel() else requestHideSelf(0)
            }

            KeyCodes.CURSOR_LEFT -> cursorController.move(-1)
            KeyCodes.CURSOR_RIGHT -> cursorController.move(1)
            KeyCodes.CURSOR_UP -> cursorController.moveVertical(-1)
            KeyCodes.CURSOR_DOWN -> cursorController.moveVertical(1)
            KeyCodes.SELECT_ALL -> cursorController.selectAll()
            KeyCodes.COPY -> copySelection()
            KeyCodes.CUT -> {
                clipboardManager.save(inputLogic.selectedText())
                cursorController.cut()
            }

            KeyCodes.PASTE -> cursorController.paste()
            KeyCodes.UNDO -> if (!inputLogic.undo()) showToast(getString(R.string.nothing_to_undo))
            KeyCodes.REDO -> if (!inputLogic.redo()) showToast(getString(R.string.nothing_to_redo))

            KeyCodes.ONE_HANDED -> {
                oneHandedManager.toggle()
                updateKeyboardMetrics()
                binding.toolbarView.setActive(
                    "one_handed", oneHandedManager.side != OneHandedModeManager.Side.OFF
                )
            }

            KeyCodes.FLOATING -> {
                floatingManager.toggle()
                updateKeyboardMetrics()
                binding.toolbarView.setActive("floating", floatingManager.isEnabled)
            }

            KeyCodes.SPLIT -> {
                splitManager.toggle()
                binding.keyboardView.setSplitEnabled(splitManager.isEnabled)
                binding.toolbarView.setActive("split", splitManager.isEnabled)
            }

            KeyCodes.INCOGNITO -> {
                val enabled = incognitoManager.toggle()
                binding.keyboardView.setIncognito(incognitoManager.isActive)
                binding.toolbarView.setActive("incognito", enabled)
                showToast(
                    getString(if (enabled) R.string.incognito_on else R.string.incognito_off)
                )
            }

            else -> Unit
        }
    }

    private fun switchMode(mode: KeyboardMode) {
        layoutManager.setMode(mode)
        reloadLayout()
    }

    /** Sends text either to the host app or to a panel's own text field. */
    private fun commitTextOrRoute(text: String) {
        if (routeText(text)) return
        inputLogic.onCharacter(text)
    }

    private fun routeText(text: String): Boolean {
        val target = activeTextTarget ?: return false
        if (!target.isAcceptingText) return false
        target.onTextCommitted(text)
        return true
    }

    private fun routeBackspace(): Boolean {
        val target = activeTextTarget ?: return false
        if (!target.isAcceptingText) return false
        target.onBackspacePressed()
        return true
    }

    private fun copySelection() {
        val selection = inputLogic.selectedText()
        if (selection.isNotBlank()) clipboardManager.save(selection)
        cursorController.copy()
    }

    // ------------------------------------------------------------------
    //  Panels
    // ------------------------------------------------------------------

    private enum class PanelType { EMOJI, CLIPBOARD, AI, MEDIA, TEXT_TOOLS, CURSOR, SEARCH, RESIZE, VOICE, SUGGESTIONS }

    private fun openPanel(type: PanelType) {
        val view: View = when (type) {
            PanelType.EMOJI -> emojiPanel ?: EmojiPanelView(this).also {
                it.listener = this
                emojiPanel = it
            }

            PanelType.CLIPBOARD -> (clipboardPanel ?: ClipboardPanelView(this).also {
                it.listener = this
                clipboardPanel = it
            }).also { it.reload() }

            PanelType.AI -> (aiPanel ?: AiPanelView(this).also {
                it.listener = this
                aiPanel = it
            }).also { it.prepare() }

            PanelType.MEDIA -> (mediaPanel ?: MediaPanelView(this).also {
                it.listener = this
                mediaPanel = it
            })

            PanelType.TEXT_TOOLS -> (textToolsPanel ?: TextToolsPanelView(this).also {
                it.listener = this
                textToolsPanel = it
            }).also { it.refresh() }

            PanelType.CURSOR -> cursorPanel ?: CursorControlPanelView(this).also {
                it.listener = CursorControlPanelView.Listener { code -> handleFunctionKey(code) }
                cursorPanel = it
            }

            PanelType.SEARCH -> (searchPanel ?: SearchPanelView(this).also {
                it.listener = this
                searchPanel = it
            }).also { it.prepare() }

            PanelType.RESIZE -> resizePanel ?: ResizePanelView(this).also {
                it.listener = this
                resizePanel = it
            }

            PanelType.VOICE -> voicePanel ?: VoiceInputView(this).also {
                it.listener = this
                voicePanel = it
            }

            PanelType.SUGGESTIONS -> suggestionsPanel ?: SuggestionsPanelView(this).also {
                it.listener = this
                suggestionsPanel = it
            }
        }
        showPanel(view)
    }

    private fun showPanel(view: View) {
        if (!::binding.isInitialized) return
        popupManager.dismissAll()
        binding.panelContainer.removeAllViews()
        binding.panelContainer.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        binding.panelContainer.visible()
        binding.keyboardView.gone()
        activePanel = view
        activeTextTarget = view as? TextInputTarget
        applyPanelTheme()
    }

    private fun hidePanel() {
        if (!::binding.isInitialized) return
        binding.panelContainer.removeAllViews()
        binding.panelContainer.gone()
        binding.keyboardView.visible()
        activePanel = null
        activeTextTarget = null
    }

    // ------------------------------------------------------------------
    //  CandidateView.Listener
    // ------------------------------------------------------------------

    override fun onSuggestionPicked(suggestion: Suggestion) {
        when (suggestion.source) {
            Suggestion.Source.CLIPBOARD -> inputLogic.commitText(suggestion.word)
            Suggestion.Source.AI -> inputLogic.commitText(suggestion.word)
            Suggestion.Source.GESTURE -> inputLogic.replaceCurrentWord(suggestion.word)
            else -> inputLogic.pickSuggestion(suggestion)
        }
        soundManager.gestureFeedback(binding.candidateView)
        updateShiftState(auto = true)
        refreshSuggestions()
    }

    override fun onSuggestionLongPressed(suggestion: Suggestion) {
        inputLogic.engine.addToDictionary(suggestion.word)
        showToast(getString(R.string.added_to_dictionary, suggestion.word))
    }

    override fun onExpandSuggestions(suggestions: List<Suggestion>) {
        openPanel(PanelType.SUGGESTIONS)
        suggestionsPanel?.setSuggestions(suggestions)
    }

    override fun onCandidateMenuClicked() = openSettings()

    override fun onExpandedSuggestionPicked(suggestion: Suggestion) {
        hidePanel()
        onSuggestionPicked(suggestion)
    }

    override fun onExpandedSuggestionsClosed() = hidePanel()

    // ------------------------------------------------------------------
    //  ToolbarView.Listener
    // ------------------------------------------------------------------

    override fun onToolbarAction(item: ToolbarItem) {
        soundManager.gestureFeedback(binding.toolbarView)
        handleFunctionKey(item.action)
    }

    // ------------------------------------------------------------------
    //  Panel listeners
    // ------------------------------------------------------------------

    override fun onEmojiPicked(emoji: String) {
        inputLogic.commitText(emoji)
        refreshSuggestions()
    }

    override fun onEmojiBackspace() {
        inputLogic.onDelete()
        refreshSuggestions()
    }

    override fun onEmojiPanelClosed() = hidePanel()

    override fun onClipPicked(text: String) {
        inputLogic.commitText(text)
        hidePanel()
        refreshSuggestions()
    }

    override fun onClipboardPanelClosed() = hidePanel()

    override fun onClipboardSettingsRequested() = openSettings(SettingsActivity.SECTION_CLIPBOARD)

    override fun onAiInsert(text: String) {
        if (text.isBlank()) return
        inputLogic.commitText(text)
        hidePanel()
    }

    override fun onAiReplace(text: String) {
        if (text.isBlank()) return
        inputLogic.replaceSelectionOrAll(text)
        hidePanel()
    }

    override fun onAiCopy(text: String) {
        if (text.isBlank()) return
        clipboardManager.copyToSystem(text)
        showToast(getString(R.string.copied_to_clipboard))
    }

    override fun onAiPanelClosed() = hidePanel()

    override fun onAiSettingsRequested() = openSettings(SettingsActivity.SECTION_AI)

    override fun aiInputText(): String = inputLogic.aiInputText()

    override fun aiContextText(): String = inputLogic.allText(2000)

    override fun onStickerPicked(sticker: String) {
        inputLogic.commitText(sticker)
        hidePanel()
    }

    override fun onGifPicked(gif: GifSearchManager.Gif) {
        scope.launch {
            val file = gifManager.download(gif)
            if (file == null) {
                inputLogic.commitText(gif.fullUrl)
                hidePanel()
                return@launch
            }
            val uri = runCatching {
                FileProvider.getUriForFile(
                    this@CustomBoardIME, "$packageName.fileprovider", file
                )
            }.getOrNull()
            val info = currentEditorInfo
            val committed = if (uri != null && info != null) {
                val description = android.content.ClipDescription(
                    gif.description.ifBlank { getString(R.string.media_gifs) },
                    arrayOf("image/gif")
                )
                val content = androidx.core.view.inputmethod.InputContentInfoCompat(
                    uri, description, null
                )
                val flags =
                    androidx.core.view.inputmethod.InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
                currentInputConnection?.let { connection ->
                    androidx.core.view.inputmethod.InputConnectionCompat.commitContent(
                        connection, info, content, flags, null
                    )
                } ?: false
            } else {
                false
            }
            if (!committed) inputLogic.commitText(gif.fullUrl)
            hidePanel()
        }
    }

    override fun onMediaPanelClosed() = hidePanel()

    override fun onMediaSettingsRequested() = openSettings(SettingsActivity.SECTION_AI)

    override fun onTextToolsReplace(text: String) {
        inputLogic.replaceSelectionOrAll(text)
        textToolsPanel?.refresh()
    }

    override fun onTextToolsClosed() = hidePanel()

    override fun textToolsInput(): String = inputLogic.aiInputText()

    override fun onSearchPanelClosed() = hidePanel()

    override fun onSearchInsert(text: String) {
        inputLogic.commitText(text)
        hidePanel()
    }

    override fun searchInitialQuery(): String =
        inputLogic.selectedText().ifBlank { inputLogic.previousWord().orEmpty() }

    override fun onKeyboardMetricsChanged() = updateKeyboardMetrics()

    override fun onResizePanelClosed() = hidePanel()

    // ------------------------------------------------------------------
    //  Voice input
    // ------------------------------------------------------------------

    private fun startVoiceInput() {
        if (!voiceManager.hasPermission()) {
            val intent = Intent(this, com.customboard.keyboard.settings.PermissionRequestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(
                    com.customboard.keyboard.settings.PermissionRequestActivity.EXTRA_PERMISSION,
                    android.Manifest.permission.RECORD_AUDIO
                )
            startActivity(intent)
            return
        }
        openPanel(PanelType.VOICE)
        voiceManager.setListener(this)
        voicePanel?.startPulsing()
        voicePanel?.setPartial("")
        voiceManager.start(layoutManager.localeTag())
    }

    override fun onVoiceReady() {
        voicePanel?.setStatus(getString(R.string.voice_listening))
    }

    override fun onVoicePartial(text: String) {
        voicePanel?.setPartial(text)
    }

    override fun onVoiceResult(text: String) {
        val commands = VoiceCommandProcessor.process(text, prefs.shortcutsEnabled)
        commands.forEach { command ->
            when (command) {
                is VoiceCommandProcessor.Command.Insert -> {
                    val needsSpace = inputLogic.textBeforeCursor(1).let {
                        it.isNotEmpty() && !it.last().isWhitespace()
                    }
                    inputLogic.commitText(if (needsSpace) " ${command.text}" else command.text)
                }

                VoiceCommandProcessor.Command.NewLine -> inputLogic.commitText("\n")
                VoiceCommandProcessor.Command.DeleteWord -> inputLogic.onDelete(wholeWord = true)
                VoiceCommandProcessor.Command.DeleteAll -> inputLogic.replaceSelectionOrAll("")
                VoiceCommandProcessor.Command.SelectAll -> cursorController.selectAll()
                VoiceCommandProcessor.Command.Undo -> inputLogic.undo()
                VoiceCommandProcessor.Command.Send -> inputLogic.onEnter(currentEditorInfo)
                VoiceCommandProcessor.Command.Stop -> stopVoiceInput()
            }
        }
        voicePanel?.setPartial("")
        refreshSuggestions()
    }

    override fun onVoiceVolume(level: Float) {
        voicePanel?.setVolume(level)
    }

    override fun onVoiceError(message: String) {
        voicePanel?.setStatus(message)
    }

    override fun onVoiceFinished() {
        voicePanel?.stopPulsing()
        handler.postDelayed({ if (activePanel === voicePanel) hidePanel() }, 400)
    }

    override fun onVoiceStopRequested() {
        voiceManager.stop()
    }

    override fun onVoiceCancelled() = stopVoiceInput()

    private fun stopVoiceInput() {
        voiceManager.cancel()
        voicePanel?.stopPulsing()
        hidePanel()
    }

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    private fun openSettings(section: String? = null) {
        val intent = Intent(this, SettingsActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (section != null) intent.putExtra(SettingsActivity.EXTRA_SECTION, section)
        runCatching { startActivity(intent) }
        requestHideSelf(0)
    }

    private fun showImePicker() {
        KeyboardUtils.showImePicker(this)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(value: Float): Float = (this as Context).dpToPx(value)
}
