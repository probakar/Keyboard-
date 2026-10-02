package com.customboard.keyboard.widgets

/**
 * Implemented by panels that own a text field (emoji search, clipboard search, AI prompt).
 *
 * An IME cannot type into its own views through the normal input path, so while such a field
 * is focused the service routes every key press here instead of to the host application.
 */
interface TextInputTarget {

    /** True while the panel's own field has focus and wants the keystrokes. */
    val isAcceptingText: Boolean

    fun onTextCommitted(text: String)

    fun onBackspacePressed()

    fun onEnterPressed()
}
