package com.customboard.keyboard.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.customboard.keyboard.utils.Defaults
import com.customboard.keyboard.utils.Prefs

/**
 * Typed wrapper around the default [SharedPreferences] file.
 *
 * The androidx preference screens write into exactly the same file, so every change made in
 * the settings app is picked up by the keyboard immediately (see [registerListener]).
 */
class PreferencesManager private constructor(context: Context) {

    val prefs: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    // ------------------------------------------------------------------
    //  Appearance
    // ------------------------------------------------------------------
    var themeId: String
        get() = prefs.getString(Prefs.THEME, Defaults.THEME) ?: Defaults.THEME
        set(value) = prefs.edit().putString(Prefs.THEME, value).apply()

    var followSystemTheme: Boolean
        get() = prefs.getBoolean(Prefs.FOLLOW_SYSTEM, true)
        set(value) = prefs.edit().putBoolean(Prefs.FOLLOW_SYSTEM, value).apply()

    var dynamicColor: Boolean
        get() = prefs.getBoolean(Prefs.DYNAMIC_COLOR, false)
        set(value) = prefs.edit().putBoolean(Prefs.DYNAMIC_COLOR, value).apply()

    var keyboardHeightPercent: Int
        get() = prefs.getInt(Prefs.KEYBOARD_HEIGHT, Defaults.KEYBOARD_HEIGHT).coerceIn(60, 170)
        set(value) = prefs.edit().putInt(Prefs.KEYBOARD_HEIGHT, value.coerceIn(60, 170)).apply()

    val keyFontSizePercent: Int
        get() = prefs.getInt(Prefs.KEY_FONT_SIZE, Defaults.KEY_FONT_SIZE).coerceIn(70, 150)

    val keyCornerRadiusDp: Int
        get() = prefs.getInt(Prefs.KEY_RADIUS, Defaults.KEY_RADIUS).coerceIn(0, 28)

    val showKeyBorders: Boolean
        get() = prefs.getBoolean(Prefs.KEY_BORDERS, false)

    val keyShape: String
        get() = prefs.getString(Prefs.KEY_SHAPE, Defaults.KEY_SHAPE) ?: Defaults.KEY_SHAPE

    val keyboardOpacity: Int
        get() = prefs.getInt(Prefs.KEYBOARD_OPACITY, Defaults.KEYBOARD_OPACITY).coerceIn(20, 100)

    var backgroundImageUri: String?
        get() = prefs.getString(Prefs.BACKGROUND_IMAGE, null)
        set(value) = prefs.edit().putString(Prefs.BACKGROUND_IMAGE, value).apply()

    val fontFamily: String
        get() = prefs.getString(Prefs.FONT, Defaults.FONT) ?: Defaults.FONT

    var bottomPaddingDp: Int
        get() = prefs.getInt(Prefs.BOTTOM_PADDING, Defaults.BOTTOM_PADDING).coerceIn(0, 48)
        set(value) = prefs.edit().putInt(Prefs.BOTTOM_PADDING, value.coerceIn(0, 48)).apply()

    val gradientBackground: Boolean
        get() = prefs.getBoolean(Prefs.GRADIENT, false)

    var customThemeJson: String?
        get() = prefs.getString(Prefs.CUSTOM_THEME, null)
        set(value) = prefs.edit().putString(Prefs.CUSTOM_THEME, value).apply()

    // ------------------------------------------------------------------
    //  Typing
    // ------------------------------------------------------------------
    val showSuggestions: Boolean get() = prefs.getBoolean(Prefs.SUGGESTIONS, true)

    /** JSON object of gesture id -> key code overrides, see CustomGestureMapper. */
    var gestureMap: String
        get() = prefs.getString(Prefs.GESTURE_MAP, "{}") ?: "{}"
        set(value) = prefs.edit().putString(Prefs.GESTURE_MAP, value).apply()

    val voiceOffline: Boolean get() = prefs.getBoolean(Prefs.VOICE_OFFLINE, false)
    val autoCorrect: Boolean get() = prefs.getBoolean(Prefs.AUTO_CORRECT, true)
    val prediction: Boolean get() = prefs.getBoolean(Prefs.PREDICTION, true)
    val smartCompose: Boolean get() = prefs.getBoolean(Prefs.SMART_COMPOSE, true)
    val autoCapitalize: Boolean get() = prefs.getBoolean(Prefs.AUTO_CAP, true)
    val doubleSpacePeriod: Boolean get() = prefs.getBoolean(Prefs.DOUBLE_SPACE_PERIOD, true)
    val smartPunctuation: Boolean get() = prefs.getBoolean(Prefs.SMART_PUNCTUATION, true)
    val smartQuotes: Boolean get() = prefs.getBoolean(Prefs.SMART_QUOTES, false)
    val autoSpace: Boolean get() = prefs.getBoolean(Prefs.AUTO_SPACE, true)
    val blockOffensive: Boolean get() = prefs.getBoolean(Prefs.BLOCK_OFFENSIVE, true)
    val gestureTyping: Boolean get() = prefs.getBoolean(Prefs.GESTURE_TYPING, true)
    val gestureTrail: Boolean get() = prefs.getBoolean(Prefs.GESTURE_TRAIL, true)
    val learnWords: Boolean get() = prefs.getBoolean(Prefs.LEARN_WORDS, true)
    val suggestContacts: Boolean get() = prefs.getBoolean(Prefs.SUGGEST_CONTACTS, false)
    val grammarCheck: Boolean get() = prefs.getBoolean(Prefs.GRAMMAR_CHECK, true)
    val spellCheck: Boolean get() = prefs.getBoolean(Prefs.SPELL_CHECK, true)
    val calculatorMode: Boolean get() = prefs.getBoolean(Prefs.CALCULATOR, true)
    val shortcutsEnabled: Boolean get() = prefs.getBoolean(Prefs.SHORTCUTS_ENABLED, true)

    var numberRow: Boolean
        get() = prefs.getBoolean(Prefs.NUMBER_ROW, false)
        set(value) = prefs.edit().putBoolean(Prefs.NUMBER_ROW, value).apply()

    var personalDictionary: Set<String>
        get() = prefs.getStringSet(Prefs.PERSONAL_DICTIONARY, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(Prefs.PERSONAL_DICTIONARY, value).apply()

    var blockedWords: Set<String>
        get() = prefs.getStringSet(Prefs.BLOCKED_WORDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(Prefs.BLOCKED_WORDS, value).apply()

    /** Serialised as `shortcut=expansion` lines. */
    var shortcuts: String
        get() = prefs.getString(Prefs.SHORTCUTS, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.SHORTCUTS, value).apply()

    // ------------------------------------------------------------------
    //  AI
    // ------------------------------------------------------------------
    val aiEnabled: Boolean get() = prefs.getBoolean(Prefs.AI_ENABLED, true)
    val aiSmartReply: Boolean get() = prefs.getBoolean(Prefs.AI_SMART_REPLY, true)
    val aiProofread: Boolean get() = prefs.getBoolean(Prefs.AI_PROOFREAD, true)
    val aiOfflineOnly: Boolean get() = prefs.getBoolean(Prefs.AI_OFFLINE_ONLY, false)
    val aiLanguageDetect: Boolean get() = prefs.getBoolean(Prefs.AI_LANGUAGE_DETECT, true)
    val aiKeepHistory: Boolean get() = prefs.getBoolean(Prefs.AI_HISTORY, true)

    var aiConsent: Boolean
        get() = prefs.getBoolean(Prefs.AI_CONSENT, false)
        set(value) = prefs.edit().putBoolean(Prefs.AI_CONSENT, value).apply()

    val aiModel: String
        get() = prefs.getString(Prefs.AI_MODEL, Defaults.AI_MODEL) ?: Defaults.AI_MODEL

    /** 0..100 slider mapped to the Gemini temperature range 0.0 .. 1.0. */
    val aiTemperature: Float
        get() = prefs.getInt(Prefs.AI_TEMPERATURE, Defaults.AI_TEMPERATURE).coerceIn(0, 100) / 100f

    val aiTone: String
        get() = prefs.getString(Prefs.AI_TONE, Defaults.TONE) ?: Defaults.TONE

    var aiTranslateTarget: String
        get() = prefs.getString(Prefs.AI_TRANSLATE_TARGET, Defaults.TRANSLATE_TARGET)
            ?: Defaults.TRANSLATE_TARGET
        set(value) = prefs.edit().putString(Prefs.AI_TRANSLATE_TARGET, value).apply()

    val aiCustomPrompts: List<String>
        get() = (prefs.getString(Prefs.AI_CUSTOM_PROMPTS, "") ?: "")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }

    // ------------------------------------------------------------------
    //  Sound & haptics
    // ------------------------------------------------------------------
    val soundEnabled: Boolean get() = prefs.getBoolean(Prefs.SOUND, false)
    val soundVolume: Int get() = prefs.getInt(Prefs.SOUND_VOLUME, Defaults.SOUND_VOLUME).coerceIn(0, 100)
    val soundProfile: String
        get() = prefs.getString(Prefs.SOUND_PROFILE, Defaults.SOUND_PROFILE) ?: Defaults.SOUND_PROFILE
    val hapticEnabled: Boolean get() = prefs.getBoolean(Prefs.HAPTIC, true)
    val vibrationStrength: Int
        get() = prefs.getInt(Prefs.VIBRATION_STRENGTH, Defaults.VIBRATION_STRENGTH).coerceIn(0, 80)
    val respectSilentMode: Boolean get() = prefs.getBoolean(Prefs.RESPECT_SILENT, true)
    val keyPreview: Boolean get() = prefs.getBoolean(Prefs.KEY_PREVIEW, true)
    val keyAnimation: Boolean get() = prefs.getBoolean(Prefs.KEY_ANIMATION, true)

    // ------------------------------------------------------------------
    //  Clipboard
    // ------------------------------------------------------------------
    val clipboardEnabled: Boolean get() = prefs.getBoolean(Prefs.CLIPBOARD_ENABLED, true)
    val clipboardSuggest: Boolean get() = prefs.getBoolean(Prefs.CLIPBOARD_SUGGEST, true)

    /** Retention in hours, or -1 for "forever". */
    val clipboardRetentionHours: Int
        get() = (prefs.getString(Prefs.CLIPBOARD_RETENTION, null)
            ?: Defaults.CLIPBOARD_RETENTION_HOURS.toString()).toIntOrNull()
            ?: Defaults.CLIPBOARD_RETENTION_HOURS

    val clipboardMaxItems: Int
        get() = (prefs.getString(Prefs.CLIPBOARD_MAX, null)
            ?: Defaults.CLIPBOARD_MAX_ITEMS.toString()).toIntOrNull() ?: Defaults.CLIPBOARD_MAX_ITEMS

    // ------------------------------------------------------------------
    //  Languages
    // ------------------------------------------------------------------
    var enabledLanguages: Set<String>
        get() {
            val stored = prefs.getStringSet(Prefs.LANGUAGES, null)
            return if (stored.isNullOrEmpty()) setOf(Defaults.LANGUAGE) else stored
        }
        set(value) = prefs.edit().putStringSet(Prefs.LANGUAGES, value).apply()

    var currentLanguage: String
        get() = prefs.getString(Prefs.CURRENT_LANGUAGE, Defaults.LANGUAGE) ?: Defaults.LANGUAGE
        set(value) = prefs.edit().putString(Prefs.CURRENT_LANGUAGE, value).apply()

    val multilingual: Boolean get() = prefs.getBoolean(Prefs.MULTILINGUAL, true)

    // ------------------------------------------------------------------
    //  Gestures
    // ------------------------------------------------------------------
    val spaceCursor: Boolean get() = prefs.getBoolean(Prefs.SPACE_CURSOR, true)
    val swipeDeleteWord: Boolean get() = prefs.getBoolean(Prefs.SWIPE_DELETE, true)
    val swipeDownToHide: Boolean get() = prefs.getBoolean(Prefs.SWIPE_DOWN_HIDE, true)
    val swipeUpForSymbols: Boolean get() = prefs.getBoolean(Prefs.SWIPE_UP_SYMBOLS, true)

    val longPressDelay: Int
        get() = (prefs.getString(Prefs.LONG_PRESS_DELAY, null)
            ?: Defaults.LONG_PRESS_DELAY.toString()).toIntOrNull() ?: Defaults.LONG_PRESS_DELAY

    val keyRepeatDelay: Int
        get() = (prefs.getString(Prefs.KEY_REPEAT_DELAY, null)
            ?: Defaults.KEY_REPEAT_DELAY.toString()).toIntOrNull() ?: Defaults.KEY_REPEAT_DELAY

    // ------------------------------------------------------------------
    //  Toolbar
    // ------------------------------------------------------------------
    val toolbarEnabled: Boolean get() = prefs.getBoolean(Prefs.TOOLBAR_ENABLED, true)

    var toolbarItems: List<String>
        get() = (prefs.getString(Prefs.TOOLBAR_ITEMS, Defaults.TOOLBAR_ITEMS)
            ?: Defaults.TOOLBAR_ITEMS).split(",").map { it.trim() }.filter { it.isNotEmpty() }
        set(value) = prefs.edit().putString(Prefs.TOOLBAR_ITEMS, value.joinToString(",")).apply()

    var toolbarExpanded: Boolean
        get() = prefs.getBoolean(Prefs.TOOLBAR_EXPANDED, true)
        set(value) = prefs.edit().putBoolean(Prefs.TOOLBAR_EXPANDED, value).apply()

    // ------------------------------------------------------------------
    //  Privacy
    // ------------------------------------------------------------------
    var incognito: Boolean
        get() = prefs.getBoolean(Prefs.INCOGNITO, false)
        set(value) = prefs.edit().putBoolean(Prefs.INCOGNITO, value).apply()

    val disableInPasswordFields: Boolean get() = prefs.getBoolean(Prefs.DISABLE_IN_PASSWORD, true)
    val appLock: Boolean get() = prefs.getBoolean(Prefs.APP_LOCK, false)

    // ------------------------------------------------------------------
    //  Modes & accessibility
    // ------------------------------------------------------------------
    var oneHandedMode: String
        get() = prefs.getString(Prefs.ONE_HANDED, "off") ?: "off"
        set(value) = prefs.edit().putString(Prefs.ONE_HANDED, value).apply()

    val oneHandedScale: Int
        get() = prefs.getInt(Prefs.ONE_HANDED_SCALE, Defaults.ONE_HANDED_SCALE).coerceIn(60, 100)

    var floatingMode: Boolean
        get() = prefs.getBoolean(Prefs.FLOATING, false)
        set(value) = prefs.edit().putBoolean(Prefs.FLOATING, value).apply()

    var floatingX: Int
        get() = prefs.getInt(Prefs.FLOATING_X, 0)
        set(value) = prefs.edit().putInt(Prefs.FLOATING_X, value).apply()

    var floatingY: Int
        get() = prefs.getInt(Prefs.FLOATING_Y, 0)
        set(value) = prefs.edit().putInt(Prefs.FLOATING_Y, value).apply()

    var splitKeyboard: Boolean
        get() = prefs.getBoolean(Prefs.SPLIT, false)
        set(value) = prefs.edit().putBoolean(Prefs.SPLIT, value).apply()

    val highContrast: Boolean get() = prefs.getBoolean(Prefs.HIGH_CONTRAST, false)
    val largeKeys: Boolean get() = prefs.getBoolean(Prefs.LARGE_KEYS, false)
    val talkbackSupport: Boolean get() = prefs.getBoolean(Prefs.TALKBACK, true)
    val rememberLayoutPerApp: Boolean get() = prefs.getBoolean(Prefs.REMEMBER_LAYOUT, true)

    // ------------------------------------------------------------------
    //  Internal state
    // ------------------------------------------------------------------
    var setupComplete: Boolean
        get() = prefs.getBoolean(Prefs.SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(Prefs.SETUP_COMPLETE, value).apply()

    var recentEmoji: String
        get() = prefs.getString(Prefs.RECENT_EMOJI, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.RECENT_EMOJI, value).apply()

    var emojiFrequency: String
        get() = prefs.getString(Prefs.EMOJI_FREQUENCY, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.EMOJI_FREQUENCY, value).apply()

    var lastEmojiCategory: Int
        get() = prefs.getInt(Prefs.LAST_EMOJI_CATEGORY, 0)
        set(value) = prefs.edit().putInt(Prefs.LAST_EMOJI_CATEGORY, value).apply()

    var skinTone: Int
        get() = prefs.getInt(Prefs.SKIN_TONE, 0).coerceIn(0, 5)
        set(value) = prefs.edit().putInt(Prefs.SKIN_TONE, value).apply()

    var layoutPerApp: String
        get() = prefs.getString(Prefs.LAST_LAYOUT_PER_APP, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.LAST_LAYOUT_PER_APP, value).apply()

    var launchCount: Int
        get() = prefs.getInt(Prefs.LAUNCH_COUNT, 0)
        set(value) = prefs.edit().putInt(Prefs.LAUNCH_COUNT, value).apply()

    var firstLaunchTime: Long
        get() = prefs.getLong(Prefs.FIRST_LAUNCH_TIME, 0L)
        set(value) = prefs.edit().putLong(Prefs.FIRST_LAUNCH_TIME, value).apply()

    var ratePromptDone: Boolean
        get() = prefs.getBoolean(Prefs.RATE_PROMPT_DONE, false)
        set(value) = prefs.edit().putBoolean(Prefs.RATE_PROMPT_DONE, value).apply()

    var lastVersionSeen: Int
        get() = prefs.getInt(Prefs.LAST_VERSION_SEEN, 0)
        set(value) = prefs.edit().putInt(Prefs.LAST_VERSION_SEEN, value).apply()

    var unigramData: String
        get() = prefs.getString(Prefs.UNIGRAM_DATA, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.UNIGRAM_DATA, value).apply()

    var bigramData: String
        get() = prefs.getString(Prefs.BIGRAM_DATA, "") ?: ""
        set(value) = prefs.edit().putString(Prefs.BIGRAM_DATA, value).apply()

    // ------------------------------------------------------------------
    //  Generic helpers
    // ------------------------------------------------------------------
    fun getString(key: String, fallback: String): String = prefs.getString(key, fallback) ?: fallback

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun getBoolean(key: String, fallback: Boolean): Boolean = prefs.getBoolean(key, fallback)

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun getInt(key: String, fallback: Int): Int = prefs.getInt(key, fallback)

    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    /** Snapshot of every preference, used by backup / restore. */
    fun snapshot(): Map<String, Any?> = prefs.all

    fun resetToDefaults() {
        val keepKeys = listOf(
            Prefs.SETUP_COMPLETE, Prefs.PERSONAL_DICTIONARY, Prefs.SHORTCUTS,
            Prefs.UNIGRAM_DATA, Prefs.BIGRAM_DATA, Prefs.AI_API_KEY
        )
        val keep = keepKeys.associateWith { prefs.all[it] }
        val editor = prefs.edit()
        editor.clear()
        keep.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.apply()
    }

    companion object {
        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context).also { instance = it }
            }
    }
}
