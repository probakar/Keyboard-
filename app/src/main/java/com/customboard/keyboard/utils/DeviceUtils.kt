package com.customboard.keyboard.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.sqrt

/** Screen size / form factor helpers used for adaptive layouts. */
object DeviceUtils {

    fun isLandscape(context: Context): Boolean =
        context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    fun isTablet(context: Context): Boolean {
        val metrics = context.resources.displayMetrics
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonal = sqrt(widthInches * widthInches + heightInches * heightInches)
        return diagonal >= 6.9 || smallestWidthDp(context) >= 600
    }

    fun smallestWidthDp(context: Context): Int = context.resources.configuration.smallestScreenWidthDp

    fun screenWidthPx(context: Context): Int = displayMetrics(context).widthPixels

    fun screenHeightPx(context: Context): Int = displayMetrics(context).heightPixels

    fun isNightMode(context: Context): Boolean =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    fun supportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Default keyboard height in pixels before user scaling is applied. */
    fun defaultKeyboardHeightPx(context: Context): Int {
        val metrics = displayMetrics(context)
        val fraction = when {
            isTablet(context) -> if (isLandscape(context)) 0.42f else 0.32f
            isLandscape(context) -> 0.52f
            else -> 0.33f
        }
        val base = (metrics.heightPixels * fraction).toInt()
        val maxHeight = (metrics.heightPixels * 0.62f).toInt()
        return min(base, maxHeight)
    }

    @Suppress("DEPRECATION")
    private fun displayMetrics(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.resources.displayMetrics
        } else {
            wm?.defaultDisplay?.getMetrics(metrics)
            if (metrics.widthPixels == 0) context.resources.displayMetrics else metrics
        }
    }
}
