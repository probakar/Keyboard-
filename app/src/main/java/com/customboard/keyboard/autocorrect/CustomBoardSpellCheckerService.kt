package com.customboard.keyboard.autocorrect

import android.service.textservice.SpellCheckerService
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo

/**
 * Exposes CustomBoard's dictionary to the whole system, so other apps get red underlines and
 * suggestions from the same engine the keyboard uses.
 */
class CustomBoardSpellCheckerService : SpellCheckerService() {

    override fun createSession(): Session = CustomBoardSession()

    private inner class CustomBoardSession : Session() {

        private lateinit var model: LanguageModelManager
        private lateinit var spellChecker: SpellChecker

        override fun onCreate() {
            model = LanguageModelManager.getInstance(applicationContext)
            model.load()
            spellChecker = SpellChecker(model)
        }

        override fun onGetSuggestions(textInfo: TextInfo?, suggestionsLimit: Int): SuggestionsInfo {
            val text = textInfo?.text?.trim().orEmpty()
            if (text.isEmpty() || !::spellChecker.isInitialized) {
                return SuggestionsInfo(0, emptyArray())
            }
            if (!spellChecker.isMisspelled(text)) {
                return SuggestionsInfo(SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY, emptyArray())
            }
            val suggestions = spellChecker
                .suggestions(text, suggestionsLimit.coerceIn(1, 5))
                .toTypedArray()
            val flags = SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
                if (suggestions.isNotEmpty()) SuggestionsInfo.RESULT_ATTR_HAS_RECOMMENDED_SUGGESTIONS else 0
            return SuggestionsInfo(flags, suggestions)
        }
    }
}
