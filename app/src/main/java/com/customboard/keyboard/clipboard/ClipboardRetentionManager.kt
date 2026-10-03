package com.customboard.keyboard.clipboard

import android.content.Context
import com.customboard.keyboard.settings.PreferencesManager
import java.util.concurrent.TimeUnit

/**
 * Enforces the retention policy: clips older than the configured window are removed
 * automatically, pinned clips are kept forever.
 */
class ClipboardRetentionManager(context: Context) {

    private val prefs = PreferencesManager.getInstance(context)
    private val dao = ClipboardDatabase.getInstance(context).clipboardDao()

    /** @return number of deleted clips. */
    suspend fun enforce(): Int {
        var removed = 0
        val hours = prefs.clipboardRetentionHours
        if (hours > 0) {
            val threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
            removed += dao.deleteOlderThan(threshold)
        }
        val max = prefs.clipboardMaxItems
        if (dao.count() > max) dao.trimTo(max)
        return removed
    }

    fun retentionLabel(): String = when (val hours = prefs.clipboardRetentionHours) {
        -1 -> "Forever"
        1 -> "1 hour"
        in 2..23 -> "$hours hours"
        24 -> "24 hours"
        else -> "${hours / 24} days"
    }
}
