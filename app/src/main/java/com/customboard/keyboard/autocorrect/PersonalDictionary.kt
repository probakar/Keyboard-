package com.customboard.keyboard.autocorrect

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager

/** The user's own words plus the block list used by "never suggest this again". */
class PersonalDictionary(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val model = LanguageModelManager.getInstance(context)

    fun words(): List<String> = prefs.personalDictionary.sorted()

    fun blocked(): List<String> = prefs.blockedWords.sorted()

    fun add(word: String) {
        val clean = word.trim()
        if (clean.isEmpty()) return
        model.addWord(clean)
        prefs.blockedWords = prefs.blockedWords - clean.lowercase()
    }

    fun remove(word: String) {
        model.removeWord(word)
    }

    fun block(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return
        prefs.blockedWords = prefs.blockedWords + clean
        model.removeWord(clean)
    }

    fun unblock(word: String) {
        prefs.blockedWords = prefs.blockedWords - word.trim().lowercase()
    }

    fun isBlocked(word: String): Boolean = prefs.blockedWords.contains(word.lowercase())

    fun clear() {
        prefs.personalDictionary = emptySet()
    }
}
