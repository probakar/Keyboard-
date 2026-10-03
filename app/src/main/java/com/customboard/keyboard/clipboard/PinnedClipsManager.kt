package com.customboard.keyboard.clipboard

import android.content.Context

/** Pinning keeps a clip out of the automatic clean-up. */
class PinnedClipsManager(context: Context) {

    private val dao = ClipboardDatabase.getInstance(context).clipboardDao()

    suspend fun pin(item: ClipboardEntity) = dao.setPinned(item.id, true)

    suspend fun unpin(item: ClipboardEntity) = dao.setPinned(item.id, false)

    suspend fun toggle(item: ClipboardEntity): Boolean {
        val pinned = !item.isPinned
        dao.setPinned(item.id, pinned)
        return pinned
    }
}
