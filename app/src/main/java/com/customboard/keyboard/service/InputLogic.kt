package com.customboard.keyboard.service

import android.content.Context
import android.text.TextUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import com.customboard.keyboard.autocorrect.AutoCorrectionEngine
import com.customboard.keyboard.autocorrect.Suggestion
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.KeyboardUtils
import com.customboard.keyboard.utils.lastWord

/**
 * All text manipulation: composing words, auto-correction, smart punctuation, double space to
 * period, undo/redo and learning. Kept separate from the service so it can be unit tested.
 */
class InputLogic(
    context: Context,
    private val connection: () -> InputConnection?
) {

    private val prefs = PreferencesManager.getInstance(context)
    private val corrections = AutoCorrectionEngine(context)

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    private data class Snapshot(val text: String, val cursor: Int)

    /** Word currently being composed (underlined in the editor). */
    var composing: String = ""
        private set

    private var lastSpaceTime = 0L
    private var justAutoCapitalised = false
    private var editorInfo: EditorInfo? = null

    val engine: AutoCorrectionEngine get() = corrections

    fun onStartInput(info: EditorInfo?) {
        editorInfo = info
        composing = ""
        undoStack.clear()
        redoStack.clear()
        corrections.ensureLoaded()
    }

    fun onFinishInput() {
        finishComposing()
        corrections.persist()
    }

    // ------------------------------------------------------------------
    //  Typing
    // ------------------------------------------------------------------

    fun onCharacter(text: String) {
        val ic = connection() ?: return
        pushUndo()
        if (!isSuggestionAllowed()) {
            ic.commitText(text, 1)
            return
        }
        val isLetter = text.length == 1 && (text[0].isLetter() || text[0] == '\'')
        if (isLetter) {
            composing += text
            ic.setComposingText(composing, 1)
        } else {
            commitComposingWithCorrection(separator = text)
            handleSeparator(ic, text)
        }
    }

    private fun handleSeparator(ic: InputConnection, text: String) {
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (prefs.smartPunctuation && text.length == 1 && text[0] in ".,;:!?" &&
            before.endsWith(" ") && !before.endsWith("  ")
        ) {
            // remove the space that precedes a punctuation mark
            ic.deleteSurroundingText(1, 0)
        }
        ic.commitText(text, 1)
        if (prefs.autoSpace && text.length == 1 && text[0] in ".,;:!?") {
            ic.commitText(" ", 1)
        }
    }

    fun onSpace(): Boolean {
        val ic = connection() ?: return false
        pushUndo()
        var doubleSpacePeriod = false
        val now = System.currentTimeMillis()
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        val hadComposingWord = composing.isNotEmpty()

        commitComposingWithCorrection(separator = " ")

        if (prefs.doubleSpacePeriod && !hadComposingWord &&
            now - lastSpaceTime < Constants.DOUBLE_TAP_TIMEOUT_MS &&
            before.length == 2 && before.endsWith(" ") && before[0].isLetterOrDigit()
        ) {
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            doubleSpacePeriod = true
            lastSpaceTime = 0
        } else {
            ic.commitText(" ", 1)
            lastSpaceTime = now
        }
        return doubleSpacePeriod
    }

    fun onDelete(wholeWord: Boolean = false) {
        val ic = connection() ?: return
        pushUndo()
        if (composing.isNotEmpty()) {
            composing = if (wholeWord) "" else composing.dropLast(1)
            if (composing.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
            } else {
                ic.setComposingText(composing, 1)
            }
            return
        }
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
            return
        }
        if (wholeWord) {
            val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            if (before.isEmpty()) return
            var count = 0
            var index = before.length - 1
            while (index >= 0 && before[index].isWhitespace()) {
                count++
                index--
            }
            while (index >= 0 && !before[index].isWhitespace()) {
                count++
                index--
            }
            ic.deleteSurroundingText(count.coerceAtLeast(1), 0)
            return
        }
        val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
        if (before.isNotEmpty() && Character.isLowSurrogate(before.last()) && before.length == 2) {
            ic.deleteSurroundingText(2, 0)
        } else {
            ic.deleteSurroundingText(1, 0)
        }
    }

    fun onEnter(info: EditorInfo?): Boolean {
        val ic = connection() ?: return false
        pushUndo()
        commitComposingWithCorrection(separator = "\n")
        val action = KeyboardUtils.editorAction(info)
        return if (info != null && !KeyboardUtils.hasNoEnterAction(info) &&
            action != EditorInfo.IME_ACTION_NONE
        ) {
            ic.performEditorAction(action)
            true
        } else {
            ic.commitText("\n", 1)
            false
        }
    }

    fun commitText(text: String, clearComposing: Boolean = true) {
        val ic = connection() ?: return
        pushUndo()
        if (clearComposing && composing.isNotEmpty()) {
            composing = ""
            ic.finishComposingText()
        }
        ic.commitText(text, 1)
    }

    /** Replaces the word around the cursor, used by gesture typing and AI rewrites. */
    fun replaceCurrentWord(word: String) {
        val ic = connection() ?: return
        pushUndo()
        if (composing.isNotEmpty()) {
            composing = ""
            ic.setComposingText("", 1)
            ic.finishComposingText()
        }
        ic.commitText(word, 1)
    }

    fun pickSuggestion(suggestion: Suggestion) {
        val ic = connection() ?: return
        pushUndo()
        val previous = previousWord()
        composing = ""
        ic.setComposingText(suggestion.word, 1)
        ic.finishComposingText()
        if (prefs.autoSpace) ic.commitText(" ", 1)
        corrections.learn(suggestion.word, previous)
    }

    fun finishComposing() {
        val ic = connection() ?: return
        if (composing.isNotEmpty()) {
            val previous = previousWord()
            corrections.learn(composing, previous)
            composing = ""
        }
        ic.finishComposingText()
    }

    private fun commitComposingWithCorrection(separator: String) {
        val ic = connection() ?: return
        if (composing.isEmpty()) return
        val previous = previousWord()
        val corrected = corrections.autoCorrectionFor(composing, previous)
        val finalWord = corrected ?: composing
        ic.setComposingText(finalWord, 1)
        ic.finishComposingText()
        corrections.learn(finalWord, previous)
        composing = ""
    }

    // ------------------------------------------------------------------
    //  Context helpers
    // ------------------------------------------------------------------

    fun textBeforeCursor(length: Int = 200): String =
        connection()?.getTextBeforeCursor(length, 0)?.toString().orEmpty()

    fun textAfterCursor(length: Int = 100): String =
        connection()?.getTextAfterCursor(length, 0)?.toString().orEmpty()

    fun selectedText(): String = connection()?.getSelectedText(0)?.toString().orEmpty()

    fun allText(limit: Int = Constants.AI_MAX_INPUT_CHARS): String {
        val ic = connection() ?: return ""
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        return extracted?.text?.toString()?.take(limit).orEmpty()
    }

    /** Text the AI should work on: the selection when there is one, otherwise everything. */
    fun aiInputText(): String {
        val selection = selectedText()
        if (selection.isNotBlank()) return selection
        val all = allText()
        return all.ifBlank { textBeforeCursor(Constants.AI_MAX_INPUT_CHARS) }
    }

    fun replaceSelectionOrAll(text: String) {
        val ic = connection() ?: return
        pushUndo()
        val selection = ic.getSelectedText(0)
        if (!selection.isNullOrEmpty()) {
            ic.commitText(text, 1)
            return
        }
        val all = allText()
        if (all.isNotEmpty()) {
            ic.performContextMenuAction(android.R.id.selectAll)
            ic.commitText(text, 1)
        } else {
            ic.commitText(text, 1)
        }
    }

    fun previousWord(): String? {
        val before = textBeforeCursor(80)
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return null
        if (before.isNotEmpty() && !before.last().isWhitespace() && composing.isNotEmpty()) {
            val withoutComposing = trimmed.dropLast(composing.length).trimEnd()
            return withoutComposing.lastWord().ifEmpty { null }
        }
        return trimmed.lastWord().ifEmpty { null }
    }

    fun suggestions(): List<Suggestion> {
        if (!isSuggestionAllowed()) return emptyList()
        return corrections.suggestions(composing, previousWord())
    }

    fun isSuggestionAllowed(): Boolean {
        val info = editorInfo ?: return prefs.showSuggestions
        if (!prefs.showSuggestions) return false
        if (KeyboardUtils.isPasswordField(info)) return false
        if (KeyboardUtils.isNoSuggestionField(info)) return false
        return true
    }

    /** Whether the next character should be upper case. */
    fun shouldAutoCapitalize(): Boolean {
        if (!prefs.autoCapitalize) return false
        val info = editorInfo ?: return false
        if (!KeyboardUtils.isAutoCapitalizedField(info)) return false
        val ic = connection() ?: return false
        val caps = ic.getCursorCapsMode(info.inputType)
        return caps != 0
    }

    fun markAutoCapitalised(value: Boolean) {
        justAutoCapitalised = value
    }

    val wasAutoCapitalised: Boolean get() = justAutoCapitalised

    // ------------------------------------------------------------------
    //  Undo / redo
    // ------------------------------------------------------------------

    private fun pushUndo() {
        val ic = connection() ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0) ?: return
        val text = extracted.text?.toString() ?: return
        if (text.length > 8000) return
        val snapshot = Snapshot(text, extracted.selectionStart)
        if (undoStack.lastOrNull()?.text == snapshot.text) return
        undoStack.addLast(snapshot)
        while (undoStack.size > Constants.MAX_UNDO_STACK) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): Boolean {
        val ic = connection() ?: return false
        val snapshot = undoStack.removeLastOrNull() ?: return false
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        extracted?.text?.toString()?.let { current ->
            redoStack.addLast(Snapshot(current, extracted.selectionStart))
        }
        applySnapshot(ic, snapshot)
        return true
    }

    fun redo(): Boolean {
        val ic = connection() ?: return false
        val snapshot = redoStack.removeLastOrNull() ?: return false
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        extracted?.text?.toString()?.let { current ->
            undoStack.addLast(Snapshot(current, extracted.selectionStart))
        }
        applySnapshot(ic, snapshot)
        return true
    }

    private fun applySnapshot(ic: InputConnection, snapshot: Snapshot) {
        ic.beginBatchEdit()
        ic.performContextMenuAction(android.R.id.selectAll)
        if (TextUtils.isEmpty(snapshot.text)) {
            ic.commitText("", 1)
        } else {
            ic.commitText(snapshot.text, 1)
        }
        ic.setSelection(
            snapshot.cursor.coerceIn(0, snapshot.text.length),
            snapshot.cursor.coerceIn(0, snapshot.text.length)
        )
        ic.endBatchEdit()
        composing = ""
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
