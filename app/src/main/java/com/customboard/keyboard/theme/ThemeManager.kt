package com.customboard.keyboard.theme

import android.content.Context
import android.content.SharedPreferences
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.DeviceUtils
import com.customboard.keyboard.utils.Prefs

/**
 * Resolves the active [ThemeColors] from the user's preferences and notifies listeners so the
 * keyboard can repaint itself instantly, without being restarted.
 */
class ThemeManager private constructor(private val context: Context) :
    SharedPreferences.OnSharedPreferenceChangeListener {

    fun interface ThemeChangeListener {
        fun onThemeChanged(theme: ThemeColors)
    }

    private val prefs = PreferencesManager.getInstance(context)
    private val listeners = mutableListOf<ThemeChangeListener>()
    private var cached: ThemeColors? = null
    private var cachedNightMode: Boolean? = null

    init {
        prefs.registerListener(this)
    }

    val current: ThemeColors
        get() {
            val night = DeviceUtils.isNightMode(context)
            val cachedTheme = cached
            if (cachedTheme != null && cachedNightMode == night) return cachedTheme
            val resolved = resolve(night)
            cached = resolved
            cachedNightMode = night
            return resolved
        }

    private fun resolve(nightMode: Boolean): ThemeColors {
        var theme = when (val id = prefs.themeId) {
            ThemePresets.ID_MATERIAL_YOU -> DynamicColorExtractor.extract(context, nightMode)
            ThemePresets.ID_CUSTOM -> CustomThemeCreator.load(context)
            else -> ThemePresets.byId(id) ?: ThemePresets.LIGHT
        }

        // "Follow system dark mode" flips between the light and dark Gboard palettes only when
        // the user stayed on one of those two themes.
        if (prefs.followSystemTheme &&
            (theme.id == ThemePresets.ID_LIGHT || theme.id == ThemePresets.ID_DARK)
        ) {
            theme = if (nightMode) ThemePresets.DARK else ThemePresets.LIGHT
        }
        if (prefs.dynamicColor && theme.id != ThemePresets.ID_CUSTOM) {
            theme = DynamicColorExtractor.extract(context, nightMode)
        }
        if (prefs.highContrast) theme = theme.highContrast()
        if (BackgroundImageManager.hasImage(context)) {
            theme = theme.withOpacity(prefs.keyboardOpacity.coerceAtMost(85))
        } else if (prefs.keyboardOpacity < 100) {
            theme = theme.withOpacity(prefs.keyboardOpacity)
        }
        return theme
    }

    fun addListener(listener: ThemeChangeListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: ThemeChangeListener) {
        listeners.remove(listener)
    }

    fun invalidate() {
        cached = null
        cachedNightMode = null
        val theme = current
        listeners.toList().forEach { it.onThemeChanged(theme) }
    }

    fun applyTheme(id: String) {
        prefs.themeId = id
        invalidate()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null) {
            invalidate()
            return
        }
        if (key in APPEARANCE_KEYS) invalidate()
    }

    companion object {
        private val APPEARANCE_KEYS = setOf(
            Prefs.THEME, Prefs.FOLLOW_SYSTEM, Prefs.DYNAMIC_COLOR, Prefs.KEYBOARD_OPACITY,
            Prefs.CUSTOM_THEME, Prefs.HIGH_CONTRAST, Prefs.GRADIENT, Prefs.BACKGROUND_IMAGE,
            Prefs.KEY_BORDERS, Prefs.KEY_SHAPE, Prefs.KEY_RADIUS, Prefs.FONT,
            Prefs.KEY_FONT_SIZE, Prefs.KEYBOARD_HEIGHT, Prefs.BOTTOM_PADDING
        )

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager =
            instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
    }
}
