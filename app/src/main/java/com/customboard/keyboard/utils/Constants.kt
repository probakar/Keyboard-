package com.customboard.keyboard.utils

/**
 * Internal key codes. Positive values are Unicode code points, negative values are
 * CustomBoard specific actions handled by [com.customboard.keyboard.service.KeyboardActionHandler].
 */
object KeyCodes {
    const val NONE = 0
    const val SPACE = 32
    const val TAB = 9
    const val NEWLINE = 10

    const val SHIFT = -1
    const val MODE_SYMBOLS = -2
    const val MODE_LETTERS = -3
    const val ENTER = -4
    const val DELETE = -5
    const val MODE_SYMBOLS_2 = -6
    const val MODE_NUMPAD = -7
    const val MODE_PHONE = -8
    const val MODE_NUMBER_ROW = -9

    const val EMOJI = -10
    const val LANGUAGE = -11
    const val SETTINGS = -12
    const val VOICE = -13
    const val CLIPBOARD = -14
    const val ONE_HANDED = -15

    const val CURSOR_LEFT = -16
    const val CURSOR_RIGHT = -17
    const val CURSOR_UP = -18
    const val CURSOR_DOWN = -19
    const val HIDE_KEYBOARD = -20

    const val AI_TOOLS = -21
    const val TEXT_TOOLS = -22
    const val SELECT_ALL = -23
    const val COPY = -24
    const val CUT = -25
    const val PASTE = -26
    const val UNDO = -27
    const val REDO = -28
    const val SEARCH = -29
    const val TRANSLATE = -30
    const val GIF = -31
    const val STICKER = -32
    const val INCOGNITO = -33
    const val FLOATING = -34
    const val SPLIT = -35
    const val RESIZE = -36
    const val THEME = -37
    const val CONTACTS = -38
    const val SHIFT_LOCK = -39
    const val BACK_TO_KEYBOARD = -40
    const val SELECT_MODE = -41
    const val DELETE_WORD = -42
    const val COMPOSE = -43
}

/** SharedPreferences keys. These are identical to the keys used in res/xml/prefs_*.xml. */
object Prefs {
    // Appearance
    const val THEME = "pref_theme"
    const val FOLLOW_SYSTEM = "pref_follow_system_theme"
    const val DYNAMIC_COLOR = "pref_dynamic_color"
    const val KEYBOARD_HEIGHT = "pref_keyboard_height"
    const val KEY_FONT_SIZE = "pref_key_font_size"
    const val KEY_RADIUS = "pref_key_radius"
    const val KEY_BORDERS = "pref_key_borders"
    const val KEY_SHAPE = "pref_key_shape"
    const val KEYBOARD_OPACITY = "pref_keyboard_opacity"
    const val BACKGROUND_IMAGE = "pref_background_image"
    const val FONT = "pref_font"
    const val BOTTOM_PADDING = "pref_bottom_padding"
    const val GRADIENT = "pref_gradient"
    const val CUSTOM_THEME = "pref_custom_theme_json"

    // Typing
    const val AUTO_CORRECT = "pref_autocorrect"
    const val PREDICTION = "pref_prediction"
    const val SMART_COMPOSE = "pref_smart_compose"
    const val AUTO_CAP = "pref_auto_cap"
    const val DOUBLE_SPACE_PERIOD = "pref_double_space_period"
    const val SMART_PUNCTUATION = "pref_smart_punctuation"
    const val SMART_QUOTES = "pref_smart_quotes"
    const val AUTO_SPACE = "pref_auto_space"
    const val BLOCK_OFFENSIVE = "pref_block_offensive"
    const val NUMBER_ROW = "pref_number_row"
    const val GESTURE_TYPING = "pref_gesture_typing"
    const val GESTURE_TRAIL = "pref_gesture_trail"
    const val LEARN_WORDS = "pref_learn_words"
    const val PERSONAL_DICTIONARY = "pref_personal_dictionary"
    const val BLOCKED_WORDS = "pref_blocked_words"
    const val SHORTCUTS = "pref_shortcuts"
    const val SHORTCUTS_ENABLED = "pref_shortcuts_enabled"
    const val SUGGEST_CONTACTS = "pref_suggest_contacts"
    const val GRAMMAR_CHECK = "pref_grammar_check"
    const val SPELL_CHECK = "pref_spell_check"
    const val CALCULATOR = "pref_calculator"
    const val SUGGESTIONS = "pref_show_suggestions"
    const val VOICE_OFFLINE = "pref_voice_offline"

    // AI
    const val AI_ENABLED = "pref_ai_enabled"
    const val AI_API_KEY = "pref_ai_api_key"
    const val AI_MODEL = "pref_ai_model"
    const val AI_TEMPERATURE = "pref_ai_temperature"
    const val AI_SMART_REPLY = "pref_ai_smart_reply"
    const val AI_PROOFREAD = "pref_ai_proofread"
    const val AI_TONE = "pref_ai_tone"
    const val AI_TRANSLATE_TARGET = "pref_ai_translate_target"
    const val AI_OFFLINE_ONLY = "pref_ai_offline_only"
    const val AI_LANGUAGE_DETECT = "pref_ai_language_detect"
    const val AI_HISTORY = "pref_ai_history"
    const val AI_CUSTOM_PROMPTS = "pref_ai_custom_prompts"
    const val AI_CONSENT = "pref_ai_consent"
    const val TENOR_API_KEY = "pref_tenor_api_key"

    // Sound & haptics
    const val SOUND = "pref_sound"
    const val SOUND_VOLUME = "pref_sound_volume"
    const val SOUND_PROFILE = "pref_sound_profile"
    const val HAPTIC = "pref_haptic"
    const val VIBRATION_STRENGTH = "pref_vibration_strength"
    const val RESPECT_SILENT = "pref_respect_silent"
    const val KEY_PREVIEW = "pref_key_preview"
    const val KEY_ANIMATION = "pref_key_animation"

    // Clipboard
    const val CLIPBOARD_ENABLED = "pref_clipboard_enabled"
    const val CLIPBOARD_RETENTION = "pref_clipboard_retention"
    const val CLIPBOARD_MAX = "pref_clipboard_max"
    const val CLIPBOARD_SUGGEST = "pref_clipboard_suggest"

    // Languages
    const val LANGUAGES = "pref_languages"
    const val CURRENT_LANGUAGE = "pref_current_language"
    const val MULTILINGUAL = "pref_multilingual"

    // Gestures
    const val SPACE_CURSOR = "pref_space_cursor"
    const val SWIPE_DELETE = "pref_swipe_delete"
    const val SWIPE_DOWN_HIDE = "pref_swipe_down_hide"
    const val SWIPE_UP_SYMBOLS = "pref_swipe_up_symbols"
    const val LONG_PRESS_DELAY = "pref_long_press_delay"
    const val KEY_REPEAT_DELAY = "pref_key_repeat_delay"
    const val GESTURE_MAP = "pref_gesture_map"

    // Toolbar
    const val TOOLBAR_ENABLED = "pref_toolbar_enabled"
    const val TOOLBAR_ITEMS = "pref_toolbar_items"
    const val TOOLBAR_EXPANDED = "pref_toolbar_expanded"

    // Privacy
    const val INCOGNITO = "pref_incognito"
    const val DISABLE_IN_PASSWORD = "pref_disable_in_password"
    const val APP_LOCK = "pref_app_lock"

    // Accessibility / modes
    const val ONE_HANDED = "pref_one_handed"
    const val ONE_HANDED_SCALE = "pref_one_handed_scale"
    const val FLOATING = "pref_floating"
    const val FLOATING_X = "pref_floating_x"
    const val FLOATING_Y = "pref_floating_y"
    const val SPLIT = "pref_split"
    const val HIGH_CONTRAST = "pref_high_contrast"
    const val LARGE_KEYS = "pref_large_keys"
    const val TALKBACK = "pref_talkback"

    // Internal state
    const val SETUP_COMPLETE = "state_setup_complete"
    const val RECENT_EMOJI = "state_recent_emoji"
    const val EMOJI_FREQUENCY = "state_emoji_frequency"
    const val LAST_EMOJI_CATEGORY = "state_last_emoji_category"
    const val SKIN_TONE = "state_skin_tone"
    const val LAST_LAYOUT_PER_APP = "state_layout_per_app"
    const val REMEMBER_LAYOUT = "pref_remember_layout"
    const val LAUNCH_COUNT = "state_launch_count"
    const val FIRST_LAUNCH_TIME = "state_first_launch"
    const val RATE_PROMPT_DONE = "state_rate_done"
    const val LAST_VERSION_SEEN = "state_last_version"
    const val BIGRAM_DATA = "state_bigrams"
    const val UNIGRAM_DATA = "state_unigrams"
}

object Defaults {
    const val KEYBOARD_HEIGHT = 100        // percent
    const val KEY_FONT_SIZE = 100          // percent
    const val KEY_RADIUS = 8               // dp
    const val KEYBOARD_OPACITY = 100       // percent
    const val BOTTOM_PADDING = 0           // dp
    const val SOUND_VOLUME = 50            // percent
    const val VIBRATION_STRENGTH = 20      // milliseconds
    const val CLIPBOARD_RETENTION_HOURS = 24
    const val CLIPBOARD_MAX_ITEMS = 500
    const val LONG_PRESS_DELAY = 300       // ms
    const val KEY_REPEAT_DELAY = 55        // ms
    const val ONE_HANDED_SCALE = 85        // percent
    const val AI_TEMPERATURE = 40          // 0..100 -> 0.0..1.0
    const val AI_MODEL = "gemini-2.5-flash"
    const val THEME = "light"
    const val TONE = "professional"
    const val TRANSLATE_TARGET = "en"
    const val LANGUAGE = "en_US"
    const val FONT = "default"
    const val KEY_SHAPE = "rounded"
    const val SOUND_PROFILE = "system"
    const val TOOLBAR_ITEMS =
        "ai,clipboard,emoji,voice,translate,text_tools,cursor,theme,gif,search,one_handed,incognito,settings"
}

object Constants {
    const val MAX_CLIPBOARD_ITEMS = 500
    const val MAX_SUGGESTIONS = 12
    const val VISIBLE_SUGGESTIONS = 3
    const val MAX_RECENT_EMOJI = 60
    const val MAX_UNDO_STACK = 50
    const val DOUBLE_TAP_TIMEOUT_MS = 350L
    const val GESTURE_MIN_DISTANCE_DP = 22f
    const val SWIPE_THRESHOLD_DP = 48f
    const val CLIPBOARD_PREVIEW_LENGTH = 140
    const val AI_MAX_INPUT_CHARS = 6000
    const val AI_TIMEOUT_SECONDS = 45L
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    const val GEMINI_KEY_URL = "https://aistudio.google.com/app/apikey"
    const val TENOR_BASE_URL = "https://tenor.googleapis.com/v2/"
    const val TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single"
    const val PROJECT_URL = "https://github.com/sufyanmoon9090/Keyboard-"
    const val SECURE_PREFS = "customboard_secure"
}
