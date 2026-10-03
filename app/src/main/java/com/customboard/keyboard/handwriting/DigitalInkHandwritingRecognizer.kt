package com.customboard.keyboard.handwriting

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import com.google.mlkit.vision.digitalink.recognition.RecognitionContext
import com.google.mlkit.vision.digitalink.recognition.WritingArea

/**
 * On-device handwriting recognition with language-specific ML Kit models. Models download only
 * when the user first writes in a language; once installed, recognition works offline.
 */
class DigitalInkHandwritingRecognizer {

    private data class Request(
        val languageCode: String,
        val languageTag: String,
        val serial: Long,
        val ink: Ink,
        val width: Float,
        val height: Float,
        val preContext: String,
        val onStatus: (String) -> Unit,
        val onCandidates: (List<String>) -> Unit
    )

    private val modelManager = RemoteModelManager.getInstance()
    private val models = HashMap<String, DigitalInkRecognitionModel>()
    private val recognizers = HashMap<String, DigitalInkRecognizer>()
    private val pending = HashMap<String, Request>()
    private val latestSerial = HashMap<String, Long>()
    private val checking = HashSet<String>()
    private val downloading = HashSet<String>()
    private var nextSerial = 0L
    private var closed = false

    fun recognize(
        languageCode: String,
        ink: Ink,
        width: Float,
        height: Float,
        preContext: String,
        onStatus: (String) -> Unit,
        onCandidates: (List<String>) -> Unit
    ) {
        if (closed) return
        val languageTag = languageTag(languageCode)
        val modelIdentifier = runCatching {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        }.getOrNull()
        if (modelIdentifier == null) {
            onStatus("${languageCode}: handwriting is not available")
            return
        }

        val request = Request(
            languageCode = languageCode,
            languageTag = languageTag,
            serial = ++nextSerial,
            ink = ink,
            width = width.coerceAtLeast(1f),
            height = height.coerceAtLeast(1f),
            preContext = preContext.takeLast(MAX_PRE_CONTEXT),
            onStatus = onStatus,
            onCandidates = onCandidates
        )
        pending[languageTag] = request
        latestSerial[languageTag] = request.serial

        recognizers[languageTag]?.let { recognizer ->
            pending.remove(languageTag)
            runRecognition(request, recognizer)
            return
        }

        val model = models.getOrPut(languageTag) {
            DigitalInkRecognitionModel.builder(modelIdentifier).build()
        }
        if (downloading.contains(languageTag) || checking.contains(languageTag)) {
            request.onStatus("Waiting for ${languageLabel(languageCode)} handwriting model…")
            return
        }

        checking += languageTag
        request.onStatus("Checking ${languageLabel(languageCode)} handwriting model…")
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { downloaded ->
                checking.remove(languageTag)
                val latest = pending[languageTag] ?: return@addOnSuccessListener
                if (closed || latest.serial != latestSerial[languageTag]) return@addOnSuccessListener
                if (downloaded) {
                    createRecognizerAndRun(latest, model)
                } else {
                    downloadModel(latest, model)
                }
            }
            .addOnFailureListener { error ->
                checking.remove(languageTag)
                val latest = pending.remove(languageTag) ?: return@addOnFailureListener
                if (!closed && latest.serial == latestSerial[languageTag]) {
                    latest.onStatus("Could not check the handwriting model: ${error.localizedMessage.orEmpty()}")
                }
            }
    }

    private fun downloadModel(request: Request, model: DigitalInkRecognitionModel) {
        val tag = request.languageTag
        if (!downloading.add(tag)) return
        request.onStatus("Downloading ${languageLabel(request.languageCode)} handwriting model…")
        modelManager.download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener {
                downloading.remove(tag)
                val latest = pending[tag] ?: return@addOnSuccessListener
                if (!closed && latest.serial == latestSerial[tag]) {
                    createRecognizerAndRun(latest, model)
                }
            }
            .addOnFailureListener { error ->
                downloading.remove(tag)
                val latest = pending.remove(tag) ?: return@addOnFailureListener
                if (!closed && latest.serial == latestSerial[tag]) {
                    latest.onStatus(
                        "Model download failed. Connect to the internet and try again. " +
                            error.localizedMessage.orEmpty()
                    )
                }
            }
    }

    private fun createRecognizerAndRun(request: Request, model: DigitalInkRecognitionModel) {
        if (closed || request.serial != latestSerial[request.languageTag]) return
        val recognizer = runCatching {
            DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build()
            )
        }.getOrElse { error ->
            pending.remove(request.languageTag)
            request.onStatus("Could not start handwriting recognition: ${error.localizedMessage.orEmpty()}")
            return
        }
        recognizers[request.languageTag] = recognizer
        pending.remove(request.languageTag)
        runRecognition(request, recognizer)
    }

    private fun runRecognition(request: Request, recognizer: DigitalInkRecognizer) {
        if (closed || request.serial != latestSerial[request.languageTag]) return
        request.onStatus("Recognizing…")
        val context = RecognitionContext.builder()
            .setPreContext(request.preContext)
            .setWritingArea(WritingArea(request.width, request.height))
            .build()
        recognizer.recognize(request.ink, context)
            .addOnSuccessListener { result ->
                if (closed || request.serial != latestSerial[request.languageTag]) return@addOnSuccessListener
                val candidates = result.candidates
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .take(MAX_CANDIDATES)
                request.onCandidates(candidates)
            }
            .addOnFailureListener { error ->
                if (!closed && request.serial == latestSerial[request.languageTag]) {
                    request.onStatus("Could not recognize that handwriting. Try writing more clearly.")
                }
            }
    }

    /** Discards in-flight or queued callbacks for a canvas that has changed or been hidden. */
    fun cancel(languageCode: String) {
        val tag = languageTag(languageCode)
        latestSerial[tag] = ++nextSerial
        pending.remove(tag)
    }

    fun close() {
        closed = true
        recognizers.values.forEach { runCatching { it.close() } }
        recognizers.clear()
        models.clear()
        pending.clear()
        checking.clear()
        downloading.clear()
    }

    private fun languageTag(code: String): String = when (code) {
        "en_US", "dvorak" -> "en-US"
        "en_GB" -> "en-GB"
        "ur" -> "ur-PK"
        "hi" -> "hi-IN"
        "ar" -> "ar"
        "fr" -> "fr-FR"
        "de" -> "de-DE"
        "es" -> "es-ES"
        else -> code.replace('_', '-')
    }

    private fun languageLabel(code: String): String = when (code) {
        "ur" -> "Urdu (Pakistan)"
        "en_US" -> "English (US)"
        "en_GB" -> "English (UK)"
        "hi" -> "Hindi"
        "ar" -> "Arabic"
        "fr" -> "French"
        "de" -> "German"
        "es" -> "Spanish"
        else -> code
    }

    companion object {
        private const val MAX_PRE_CONTEXT = 20
        private const val MAX_CANDIDATES = 5
    }
}
