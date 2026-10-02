package com.customboard.keyboard.ai

import android.util.Log
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-device language identification (ML Kit). Used to offer "switch to Urdu?" when the user
 * starts typing in another language, and to pick the source language for translation.
 */
class LanguageDetector {

    private val client by lazy { LanguageIdentification.getClient() }

    /** BCP-47 tag, or null when the text is too short or undetermined. */
    suspend fun detect(text: String): String? = withContext(Dispatchers.Default) {
        if (text.trim().length < 8) return@withContext null
        runCatching {
            val code = client.identifyLanguage(text.take(400)).await()
            if (code == LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG) null else code
        }.onFailure { Log.w(TAG, "language id failed", it) }.getOrNull()
    }

    suspend fun detectAll(text: String): List<Pair<String, Float>> = withContext(Dispatchers.Default) {
        runCatching {
            client.identifyPossibleLanguages(text.take(400)).await()
                .filter { it.languageTag != LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG }
                .map { it.languageTag to it.confidence }
        }.getOrDefault(emptyList())
    }

    fun release() = runCatching { client.close() }

    companion object {
        private const val TAG = "LanguageDetector"
    }
}
