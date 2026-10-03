package com.customboard.keyboard.settings

import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import com.customboard.keyboard.R
import com.customboard.keyboard.autocorrect.LanguageModelManager
import com.customboard.keyboard.clipboard.ClipboardManager
import com.customboard.keyboard.privacy.SecureStorage
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.KeyboardUtils
import com.customboard.keyboard.utils.Prefs

/**
 * One fragment drives every preference screen; the XML resource is chosen from the arguments.
 * Preferences that need code (API keys, dictionary management, links) are wired here.
 */
open class SettingsFragment : PreferenceFragmentCompat() {

    /** Preference XML backing this screen; subclasses declared in XML override it. */
    open val screenRes: Int
        get() = arguments?.getInt(ARG_SCREEN, R.xml.preferences_root) ?: R.xml.preferences_root

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(screenRes, rootKey)
        wireActions()
    }

    override fun onResume() {
        super.onResume()
        updateSummaries()
    }

    private fun wireActions() {
        findPreference<Preference>(KEY_ENABLE_KEYBOARD)?.setOnPreferenceClickListener {
            KeyboardUtils.openImeSettings(requireContext())
            true
        }
        findPreference<Preference>(KEY_SELECT_KEYBOARD)?.setOnPreferenceClickListener {
            KeyboardUtils.showImePicker(requireContext())
            true
        }
        findPreference<Preference>(KEY_SETUP_WIZARD)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), SetupActivity::class.java))
            true
        }
        findPreference<Preference>(KEY_THEME_PICKER)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), ThemePickerActivity::class.java))
            true
        }
        findPreference<Preference>(KEY_TOOLBAR_CUSTOMIZE)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), ToolbarCustomizeActivity::class.java))
            true
        }
        findPreference<Preference>(KEY_DICTIONARY)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), PersonalDictionaryActivity::class.java))
            true
        }
        findPreference<Preference>(Prefs.AI_API_KEY)?.setOnPreferenceClickListener {
            ApiKeyDialogFragment.newInstance(ApiKeyDialogFragment.TYPE_GEMINI)
                .show(parentFragmentManager, "gemini_key")
            true
        }
        findPreference<Preference>(KEY_TENOR_API_KEY)?.setOnPreferenceClickListener {
            ApiKeyDialogFragment.newInstance(ApiKeyDialogFragment.TYPE_TENOR)
                .show(parentFragmentManager, "tenor_key")
            true
        }
        findPreference<Preference>(KEY_GET_API_KEY)?.setOnPreferenceClickListener {
            openUrl(Constants.GEMINI_KEY_URL)
            true
        }
        findPreference<Preference>(KEY_CLEAR_CLIPBOARD)?.setOnPreferenceClickListener {
            ClipboardManager.getInstance(requireContext()).clearAll()
            toast(getString(R.string.clipboard_cleared))
            true
        }
        findPreference<Preference>(KEY_CLEAR_LEARNED)?.setOnPreferenceClickListener {
            LanguageModelManager.getInstance(requireContext()).clearLearnedData()
            toast(getString(R.string.learned_data_cleared))
            true
        }
        findPreference<Preference>(KEY_RESET_SETTINGS)?.setOnPreferenceClickListener {
            PreferencesManager.getInstance(requireContext()).resetToDefaults()
            activity?.recreate()
            true
        }
        findPreference<Preference>(KEY_PRIVACY_POLICY)?.setOnPreferenceClickListener {
            openUrl(Constants.PROJECT_URL)
            true
        }
        findPreference<Preference>(KEY_SOURCE_CODE)?.setOnPreferenceClickListener {
            openUrl(Constants.PROJECT_URL)
            true
        }
        findPreference<Preference>(KEY_VERSION)?.summary =
            com.customboard.keyboard.BuildConfig.VERSION_NAME
    }

    private fun updateSummaries() {
        val context = requireContext()
        val secure = SecureStorage.getInstance(context)
        findPreference<Preference>(Prefs.AI_API_KEY)?.summary = getString(
            if (secure.geminiApiKey.isBlank()) R.string.ai_key_not_set else R.string.ai_key_set
        )
        findPreference<Preference>(KEY_TENOR_API_KEY)?.summary = getString(
            if (secure.tenorApiKey.isBlank()) R.string.ai_key_not_set else R.string.ai_key_set
        )
        findPreference<Preference>(KEY_ENABLE_KEYBOARD)?.summary = getString(
            if (KeyboardUtils.isImeEnabled(context)) R.string.setup_enabled else R.string.setup_not_enabled
        )
        findPreference<Preference>(KEY_SELECT_KEYBOARD)?.summary = getString(
            if (KeyboardUtils.isImeSelected(context)) R.string.setup_selected else R.string.setup_not_selected
        )
        listOf(
            Prefs.THEME, Prefs.AI_MODEL, Prefs.AI_TONE, Prefs.AI_TRANSLATE_TARGET,
            Prefs.KEY_SHAPE, Prefs.FONT, Prefs.SOUND_PROFILE, Prefs.CLIPBOARD_RETENTION,
            Prefs.CLIPBOARD_MAX, Prefs.ONE_HANDED, Prefs.LONG_PRESS_DELAY,
            Prefs.KEY_REPEAT_DELAY, Prefs.CURRENT_LANGUAGE
        ).forEach { key ->
            findPreference<ListPreference>(key)?.let { preference ->
                preference.summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
            }
        }
        listOf(
            Prefs.KEYBOARD_HEIGHT, Prefs.KEY_FONT_SIZE, Prefs.KEY_RADIUS, Prefs.BOTTOM_PADDING,
            Prefs.KEYBOARD_OPACITY, Prefs.SOUND_VOLUME, Prefs.VIBRATION_STRENGTH,
            Prefs.ONE_HANDED_SCALE, Prefs.AI_TEMPERATURE
        ).forEach { key ->
            findPreference<SeekBarPreference>(key)?.showSeekBarValue = true
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun toast(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    // ------------------------------------------------------------------

    class Typing : SettingsFragment() {
        override val screenRes = R.xml.preferences_typing
    }

    class Theme : SettingsFragment() {
        override val screenRes = R.xml.preferences_theme
    }

    class Ai : SettingsFragment() {
        override val screenRes = R.xml.preferences_ai
    }

    class Clipboard : SettingsFragment() {
        override val screenRes = R.xml.preferences_clipboard
    }

    class Languages : SettingsFragment() {
        override val screenRes = R.xml.preferences_languages
    }

    class Gestures : SettingsFragment() {
        override val screenRes = R.xml.preferences_gestures
    }

    class SoundHaptics : SettingsFragment() {
        override val screenRes = R.xml.preferences_sound
    }

    class LayoutModes : SettingsFragment() {
        override val screenRes = R.xml.preferences_layout
    }

    class Privacy : SettingsFragment() {
        override val screenRes = R.xml.preferences_privacy
    }

    class Accessibility : SettingsFragment() {
        override val screenRes = R.xml.preferences_accessibility
    }

    class About : SettingsFragment() {
        override val screenRes = R.xml.preferences_about
    }

    companion object {
        const val ARG_SCREEN = "arg_screen"

        const val KEY_ENABLE_KEYBOARD = "action_enable_keyboard"
        const val KEY_SELECT_KEYBOARD = "action_select_keyboard"
        const val KEY_SETUP_WIZARD = "action_setup_wizard"
        const val KEY_THEME_PICKER = "action_theme_picker"
        const val KEY_TOOLBAR_CUSTOMIZE = "action_toolbar_customize"
        const val KEY_DICTIONARY = "action_personal_dictionary"
        const val KEY_TENOR_API_KEY = "action_tenor_key"
        const val KEY_GET_API_KEY = "action_get_api_key"
        const val KEY_CLEAR_CLIPBOARD = "action_clear_clipboard"
        const val KEY_CLEAR_LEARNED = "action_clear_learned"
        const val KEY_RESET_SETTINGS = "action_reset_settings"
        const val KEY_PRIVACY_POLICY = "action_privacy_policy"
        const val KEY_SOURCE_CODE = "action_source_code"
        const val KEY_VERSION = "action_version"

        fun forSection(section: String?): SettingsFragment {
            val screen = when (section) {
                SettingsActivity.SECTION_THEME -> R.xml.preferences_theme
                SettingsActivity.SECTION_AI -> R.xml.preferences_ai
                SettingsActivity.SECTION_CLIPBOARD -> R.xml.preferences_clipboard
                SettingsActivity.SECTION_LAYOUT -> R.xml.preferences_layout
                SettingsActivity.SECTION_TYPING -> R.xml.preferences_typing
                SettingsActivity.SECTION_LANGUAGES -> R.xml.preferences_languages
                else -> R.xml.preferences_root
            }
            return SettingsFragment().apply {
                arguments = Bundle().apply { putInt(ARG_SCREEN, screen) }
            }
        }
    }
}
