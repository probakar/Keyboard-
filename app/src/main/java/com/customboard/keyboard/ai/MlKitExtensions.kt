package com.customboard.keyboard.ai

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges Google Play services [Task]s to coroutines without pulling in an extra artifact.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    if (isComplete) {
        val exception = exception
        if (exception == null) {
            if (isCanceled) continuation.cancel() else continuation.resume(result)
        } else {
            continuation.resumeWithException(exception)
        }
        return@suspendCancellableCoroutine
    }
    addOnSuccessListener { value -> continuation.resume(value) }
    addOnFailureListener { error -> continuation.resumeWithException(error) }
    addOnCanceledListener { continuation.cancel() }
}
