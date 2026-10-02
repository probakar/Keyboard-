package com.customboard.keyboard.ai

import android.util.LruCache

/** Avoids paying twice for the same request (e.g. tapping "rewrite" again on the same text). */
object AiResultCache {

    private val cache = LruCache<String, String>(32)

    fun get(action: String, input: String, extra: String = ""): String? =
        cache.get(key(action, input, extra))

    fun put(action: String, input: String, result: String, extra: String = "") {
        if (input.isBlank() || result.isBlank()) return
        cache.put(key(action, input, extra), result)
    }

    fun clear() = cache.evictAll()

    private fun key(action: String, input: String, extra: String) =
        "$action|$extra|${input.take(400)}"
}
