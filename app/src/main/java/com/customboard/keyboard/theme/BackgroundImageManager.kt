package com.customboard.keyboard.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.customboard.keyboard.settings.PreferencesManager
import java.io.File
import java.io.FileOutputStream

/** Stores and loads the optional custom keyboard background image. */
object BackgroundImageManager {

    private const val TAG = "BackgroundImage"
    private const val FILE_NAME = "keyboard_background.jpg"
    private const val MAX_DIMENSION = 1280

    private var cached: Bitmap? = null
    private var cachedPath: String? = null

    fun file(context: Context): File = File(context.applicationContext.filesDir, FILE_NAME)

    /** Copies the picked image into app storage (scaled down) and remembers it. */
    fun saveFromUri(context: Context, uri: Uri): Boolean = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        if (bitmap == null) {
            false
        } else {
            FileOutputStream(file(context)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
            cached = null
            cachedPath = null
            PreferencesManager.getInstance(context).backgroundImageUri = file(context).absolutePath
            true
        }
    } catch (e: Exception) {
        Log.w(TAG, "Unable to save background image", e)
        false
    }

    fun load(context: Context): Bitmap? {
        val path = PreferencesManager.getInstance(context).backgroundImageUri ?: return null
        if (path == cachedPath && cached?.isRecycled == false) return cached
        val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        cached = bitmap
        cachedPath = path
        return bitmap
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
        cached?.recycle()
        cached = null
        cachedPath = null
        PreferencesManager.getInstance(context).backgroundImageUri = null
    }

    fun hasImage(context: Context): Boolean =
        PreferencesManager.getInstance(context).backgroundImageUri != null && file(context).exists()

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w > MAX_DIMENSION || h > MAX_DIMENSION) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }
}
