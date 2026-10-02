package com.customboard.keyboard.settings

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.customboard.keyboard.R
import com.customboard.keyboard.privacy.SecureStorage
import com.customboard.keyboard.utils.dpToPx

/** Collects an API key and stores it with [SecureStorage] (encrypted, never synced). */
class ApiKeyDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val type = arguments?.getString(ARG_TYPE) ?: TYPE_GEMINI
        val storage = SecureStorage.getInstance(context)

        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setSingleLine()
            hint = getString(R.string.ai_key_hint)
            setText(if (type == TYPE_GEMINI) storage.geminiApiKey else storage.tenorApiKey)
        }
        val container = FrameLayout(context).apply {
            val padding = context.dpToPx(20f).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        return AlertDialog.Builder(context)
            .setTitle(
                if (type == TYPE_GEMINI) R.string.ai_api_key_title else R.string.tenor_api_key_title
            )
            .setMessage(
                if (type == TYPE_GEMINI) R.string.ai_api_key_message else R.string.tenor_api_key_message
            )
            .setView(container)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val value = input.text?.toString()?.trim().orEmpty()
                if (type == TYPE_GEMINI) storage.geminiApiKey = value
                else storage.tenorApiKey = value
            }
            .setNeutralButton(R.string.action_clear) { _, _ ->
                if (type == TYPE_GEMINI) storage.geminiApiKey = "" else storage.tenorApiKey = ""
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }

    companion object {
        const val TYPE_GEMINI = "gemini"
        const val TYPE_TENOR = "tenor"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(type: String): ApiKeyDialogFragment = ApiKeyDialogFragment().apply {
            arguments = Bundle().apply { putString(ARG_TYPE, type) }
        }
    }
}
