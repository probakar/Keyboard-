package com.customboard.keyboard.ai

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Offline translation with ML Kit. Models are downloaded once per language pair (about 30 MB)
 * and then everything runs on the device - no text ever leaves the phone.
 */
class OnDeviceTranslator {

    private val translators = HashMap<String, Translator>()

    sealed class Outcome {
        data class Success(val text: String) : Outcome()
        data class ModelMissing(val language: String) : Outcome()
        data class Failure(val message: String) : Outcome()
    }

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null,
        allowDownload: Boolean = true
    ): Outcome = withContext(Dispatchers.Default) {
        if (text.isBlank()) return@withContext Outcome.Failure("empty")
        val target = TranslateLanguage.fromLanguageTag(targetLanguage)
            ?: return@withContext Outcome.Failure(targetLanguage)
        val source = sourceLanguage?.let { TranslateLanguage.fromLanguageTag(it) }
            ?: TranslateLanguage.ENGLISH

        val key = "$source>$target"
        val translator = translators.getOrPut(key) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(source)
                    .setTargetLanguage(target)
                    .build()
            )
        }

        runCatching {
            val conditions = DownloadConditions.Builder().apply {
                if (!allowDownload) requireWifi()
            }.build()
            translator.downloadModelIfNeeded(conditions).await()
            Outcome.Success(translator.translate(text.take(4000)).await())
        }.getOrElse { error ->
            Log.w(TAG, "translation failed", error)
            Outcome.Failure(error.localizedMessage ?: "error")
        }
    }

    fun release() {
        translators.values.forEach { runCatching { it.close() } }
        translators.clear()
    }

    companion object {
        private const val TAG = "OnDeviceTranslator"

        /** Languages ML Kit can translate offline, mapped to the tags used in settings. */
        fun supports(languageTag: String): Boolean =
            TranslateLanguage.fromLanguageTag(languageTag) != null
    }
}
