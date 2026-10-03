package com.customboard.keyboard.service

import android.view.KeyEvent
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * Everything that moves the caret or changes the selection: arrow keys, the space bar slide,
 * word jumps, select all and the clipboard shortcuts.
 */
class CursorController(private val connection: () -> InputConnection?) {

    var selectionStart = 0
        private set
    var selectionEnd = 0
        private set

    /** True while the user is in "selection" mode, where moving the caret extends the range. */
    var selectionMode = false

    private var slideAnchor = 0

    fun onUpdateSelection(newStart: Int, newEnd: Int) {
        selectionStart = newStart
        selectionEnd = newEnd
    }

    val hasSelection: Boolean get() = selectionEnd != selectionStart

    fun move(dx: Int, extend: Boolean = selectionMode) {
        val ic = connection() ?: return
        val keyCode = if (dx < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        val meta = if (extend) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        repeat(kotlin.math.abs(dx)) {
            sendKey(ic, keyCode, meta)
        }
    }

    fun moveVertical(dy: Int, extend: Boolean = selectionMode) {
        val ic = connection() ?: return
        val keyCode = if (dy < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        val meta = if (extend) KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON else 0
        repeat(kotlin.math.abs(dy)) { sendKey(ic, keyCode, meta) }
    }

    fun moveToLineStart(extend: Boolean = selectionMode) {
        val ic = connection() ?: return
        sendKey(
            ic, KeyEvent.KEYCODE_MOVE_HOME,
            if (extend) KeyEvent.META_SHIFT_ON else 0
        )
    }

    fun moveToLineEnd(extend: Boolean = selectionMode) {
        val ic = connection() ?: return
        sendKey(ic, KeyEvent.KEYCODE_MOVE_END, if (extend) KeyEvent.META_SHIFT_ON else 0)
    }

    /** Jumps over a whole word, used by long pressing the arrow keys. */
    fun moveWord(forward: Boolean, extend: Boolean = selectionMode) {
        val ic = connection() ?: return
        val steps = if (forward) {
            val after = ic.getTextAfterCursor(64, 0)?.toString().orEmpty()
            wordBoundary(after)
        } else {
            val before = ic.getTextBeforeCursor(64, 0)?.toString().orEmpty()
            wordBoundary(before.reversed())
        }
        move(if (forward) steps else -steps, extend)
    }

    private fun wordBoundary(text: String): Int {
        if (text.isEmpty()) return 0
        var index = 0
        while (index < text.length && text[index].isWhitespace()) index++
        while (index < text.length && !text[index].isWhitespace()) index++
        return index.coerceAtLeast(1)
    }

    fun selectAll() {
        connection()?.performContextMenuAction(android.R.id.selectAll)
    }

    fun copy() = connection()?.performContextMenuAction(android.R.id.copy)

    fun cut() = connection()?.performContextMenuAction(android.R.id.cut)

    fun paste() = connection()?.performContextMenuAction(android.R.id.paste)

    fun deselect() {
        selectionMode = false
        val ic = connection() ?: return
        if (hasSelection) ic.setSelection(selectionEnd, selectionEnd)
    }

    // ------------------------------------------------------------------
    //  Space bar slide
    // ------------------------------------------------------------------

    fun beginSlide() {
        slideAnchor = selectionStart
    }

    /** @param steps signed number of characters the finger has travelled */
    fun slideTo(steps: Int) {
        val ic = connection() ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
        val length = extracted?.text?.length ?: return
        val target = (slideAnchor + steps).coerceIn(0, length)
        ic.setSelection(target, target)
    }

    fun endSlide() {
        slideAnchor = selectionStart
    }

    private fun sendKey(ic: InputConnection, keyCode: Int, meta: Int) {
        val now = System.currentTimeMillis()
        ic.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta)
        )
        ic.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta)
        )
    }
}
