package com.customboard.keyboard.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.settings.PreferencesManager
import java.util.Locale

/**
 * Voice typing built on Android's on-device/cloud speech recogniser (Google Speech Services).
 * Partial results stream into the text field while the user is still speaking.
 */
class VoiceInputManager(private val context: Context) {

    interface Listener {
        fun onVoiceReady()
        fun onVoicePartial(text: String)
        fun onVoiceResult(text: String)
        fun onVoiceVolume(level: Float)
        fun onVoiceError(message: String)
        fun onVoiceFinished()
    }

    private val prefs = PreferencesManager.getInstance(context)
    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null

    var isListening = false
        private set

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun start(languageTag: String = Locale.getDefault().toLanguageTag()) {
        if (isListening) return
        if (!isAvailable()) {
            listener?.onVoiceError(context.getString(R.string.voice_not_available))
            return
        }
        if (!hasPermission()) {
            listener?.onVoiceError(context.getString(R.string.voice_permission_required))
            return
        }
        release()
        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = speechRecognizer
        speechRecognizer.setRecognitionListener(recognitionListener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, prefs.voiceOffline)
            }
        }
        runCatching {
            speechRecognizer.startListening(intent)
            isListening = true
        }.onFailure {
            listener?.onVoiceError(context.getString(R.string.voice_error_generic))
        }
    }

    fun stop() {
        runCatching { recognizer?.stopListening() }
        isListening = false
    }

    fun cancel() {
        runCatching { recognizer?.cancel() }
        isListening = false
        listener?.onVoiceFinished()
    }

    fun release() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        isListening = false
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            listener?.onVoiceReady()
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            listener?.onVoiceVolume(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            isListening = false
        }

        override fun onError(error: Int) {
            isListening = false
            listener?.onVoiceError(messageFor(error))
            listener?.onVoiceFinished()
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) listener?.onVoiceResult(text)
            listener?.onVoiceFinished()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isNotBlank()) listener?.onVoicePartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun messageFor(error: Int): String = context.getString(
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> R.string.voice_error_audio
            SpeechRecognizer.ERROR_CLIENT -> R.string.voice_error_generic
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_permission_required
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                R.string.voice_error_network
            SpeechRecognizer.ERROR_NO_MATCH -> R.string.voice_error_no_match
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.voice_error_busy
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.voice_error_no_speech
            else -> R.string.voice_error_generic
        }
    )
}
