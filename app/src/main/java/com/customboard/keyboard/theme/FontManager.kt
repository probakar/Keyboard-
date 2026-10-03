package com.customboard.keyboard.theme

import android.content.Context
import android.graphics.Typeface
import com.customboard.keyboard.settings.PreferencesManager

/** Resolves the user's font choice into a [Typeface]. */
object FontManager {

    private val cache = HashMap<String, Typeface>()

    fun typeface(context: Context): Typeface {
        val family = PreferencesManager.getInstance(context).fontFamily
        return typefaceFor(family)
    }

    fun typefaceFor(family: String): Typeface = cache.getOrPut(family) {
        when (family) {
            "sans-serif" -> Typeface.create("sans-serif", Typeface.NORMAL)
            "sans-serif-medium" -> Typeface.create("sans-serif-medium", Typeface.NORMAL)
            "sans-serif-condensed" -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
            "serif" -> Typeface.create("serif", Typeface.NORMAL)
            "monospace" -> Typeface.MONOSPACE
            "casual" -> Typeface.create("casual", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
    }

    fun boldVariant(base: Typeface): Typeface = Typeface.create(base, Typeface.BOLD)
}
