package com.customboard.keyboard.clipboard

import android.content.ClipData
import android.content.Context
import android.util.Log
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.isEmailAddress
import com.customboard.keyboard.utils.isNumeric
import com.customboard.keyboard.utils.isPhoneNumber
import com.customboard.keyboard.utils.isUrl
import com.customboard.keyboard.utils.truncate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Captures everything the user copies while CustomBoard is the active keyboard and keeps it in
 * the Room database until the retention window expires.
 *
 * Nothing is recorded in incognito mode, and nothing ever leaves the device.
 */
class ClipboardManager private constructor(private val context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val dao = ClipboardDatabase.getInstance(context).clipboardDao()
    private val retention = ClipboardRetentionManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val systemClipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager

    private var listenerRegistered = false
    private var lastCaptured: String? = null

    /** Most recent clip, used for the "paste" chip in the suggestion strip. */
    @Volatile
    var latestClip: String? = null
        private set

    private val clipListener =
        android.content.ClipboardManager.OnPrimaryClipChangedListener { captureCurrentClip() }

    fun observeAll(): Flow<List<ClipboardEntity>> = dao.observeAll()

    // ------------------------------------------------------------------
    //  Monitoring
    // ------------------------------------------------------------------

    fun startMonitoring() {
        if (listenerRegistered) return
        runCatching {
            systemClipboard?.addPrimaryClipChangedListener(clipListener)
            listenerRegistered = true
        }
        cleanUp()
        captureCurrentClip()
    }

    fun stopMonitoring() {
        if (!listenerRegistered) return
        runCatching { systemClipboard?.removePrimaryClipChangedListener(clipListener) }
        listenerRegistered = false
    }

    fun captureCurrentClip() {
        if (!prefs.clipboardEnabled || prefs.incognito) return
        val clip = runCatching { systemClipboard?.primaryClip }.getOrNull() ?: return
        if (clip.itemCount == 0) return
        val text = runCatching { clip.getItemAt(0).coerceToText(context)?.toString() }
            .getOrNull()?.trim().orEmpty()
        if (text.isEmpty() || text.length > 20_000) return
        if (text == lastCaptured) return
        lastCaptured = text
        latestClip = text
        save(text)
    }

    fun save(text: String) {
        if (!prefs.clipboardEnabled || prefs.incognito) return
        val clean = text.trim()
        if (clean.isEmpty()) return
        scope.launch {
            runCatching {
                val existing = dao.findByContent(clean)
                if (existing != null) {
                    dao.update(existing.copy(timestamp = System.currentTimeMillis()))
                } else {
                    dao.insert(
                        ClipboardEntity(
                            content = clean,
                            timestamp = System.currentTimeMillis(),
                            category = categorize(clean),
                            preview = clean.truncate(Constants.CLIPBOARD_PREVIEW_LENGTH)
                        )
                    )
                }
                retention.enforce()
            }.onFailure { Log.w(TAG, "save failed", it) }
        }
    }

    // ------------------------------------------------------------------
    //  Operations used by the clipboard panel
    // ------------------------------------------------------------------

    suspend fun recent(limit: Int = 100): List<ClipboardEntity> = withContext(Dispatchers.IO) {
        runCatching { dao.recent(limit) }.getOrDefault(emptyList())
    }

    suspend fun search(query: String): List<ClipboardEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) recent() else runCatching { dao.search(query.trim()) }
            .getOrDefault(emptyList())
    }

    suspend fun byCategory(category: String): List<ClipboardEntity> = withContext(Dispatchers.IO) {
        if (category == CATEGORY_ALL) recent()
        else runCatching { dao.byCategory(category) }.getOrDefault(emptyList())
    }

    fun togglePin(item: ClipboardEntity) {
        scope.launch { runCatching { dao.setPinned(item.id, !item.isPinned) } }
    }

    fun delete(item: ClipboardEntity) {
        scope.launch { runCatching { dao.delete(item.id) } }
    }

    fun edit(item: ClipboardEntity, newContent: String) {
        scope.launch {
            runCatching {
                dao.updateContent(
                    item.id, newContent, newContent.truncate(Constants.CLIPBOARD_PREVIEW_LENGTH)
                )
            }
        }
    }

    fun clearAll(onDone: (() -> Unit)? = null) {
        scope.launch {
            runCatching { dao.deleteAll() }
            latestClip = null
            lastCaptured = null
            withContext(Dispatchers.Main) { onDone?.invoke() }
        }
    }

    fun cleanUp() {
        scope.launch { runCatching { retention.enforce() } }
    }

    /** Copies [text] to the system clipboard (and therefore into the history). */
    fun copyToSystem(text: String) {
        runCatching {
            systemClipboard?.setPrimaryClip(ClipData.newPlainText("CustomBoard", text))
        }
    }

    fun systemClipText(): String? = runCatching {
        systemClipboard?.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(context)?.toString()
    }.getOrNull()

    // ------------------------------------------------------------------
    //  Helpers
    // ------------------------------------------------------------------

    fun categorize(text: String): String {
        val trimmed = text.trim()
        return when {
            trimmed.isEmailAddress() -> ClipboardEntity.CATEGORY_EMAIL
            trimmed.isUrl() -> ClipboardEntity.CATEGORY_LINK
            trimmed.isPhoneNumber() -> ClipboardEntity.CATEGORY_PHONE
            trimmed.isNumeric() && trimmed.length <= 10 -> ClipboardEntity.CATEGORY_NUMBER
            looksLikeCode(trimmed) -> ClipboardEntity.CATEGORY_CODE
            looksLikeAddress(trimmed) -> ClipboardEntity.CATEGORY_ADDRESS
            else -> ClipboardEntity.CATEGORY_TEXT
        }
    }

    private fun looksLikeCode(text: String): Boolean {
        if (text.length > 24) return false
        val hasDigit = text.any { it.isDigit() }
        val hasLetter = text.any { it.isLetter() }
        return hasDigit && hasLetter && text.none { it.isWhitespace() }
    }

    private fun looksLikeAddress(text: String): Boolean {
        val lower = text.lowercase()
        val markers = listOf(
            "street", "road", "avenue", "block", "house", "sector", "town", "city", "st.", "rd."
        )
        return text.length in 12..200 && markers.any { lower.contains(it) }
    }

    companion object {
        private const val TAG = "ClipboardManager"
        const val CATEGORY_ALL = "all"

        @Volatile
        private var instance: ClipboardManager? = null

        fun getInstance(context: Context): ClipboardManager =
            instance ?: synchronized(this) {
                instance ?: ClipboardManager(context.applicationContext).also { instance = it }
            }
    }
}
