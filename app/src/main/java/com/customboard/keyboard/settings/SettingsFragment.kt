package com.customboard.keyboard.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
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

    private var searchEmptyPreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(screenRes, rootKey)
        wireActions()
        (activity as? SettingsActivity)?.let { filterPreferences(it.currentSearchQuery) }
    }

    override fun onResume() {
        super.onResume()
        updateSummaries()
    }

    private fun wireActions() {
        configureAppLockPreference()
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
        findPreference<Preference>(KEY_EDIT_SHORTCUTS)?.setOnPreferenceClickListener {
            showTextShortcutEditor()
            true
        }
        findPreference<Preference>(Prefs.AI_API_KEY)?.setOnPreferenceClickListener {
            ApiKeyDialogFragment.newInstance(ApiKeyDialogFragment.TYPE_GEMINI)
                .show(parentFragmentManager, "gemini_key")
            true
        }
        findPreference<Preference>(KEY_GIPHY_API_KEY)?.setOnPreferenceClickListener {
            ApiKeyDialogFragment.newInstance(ApiKeyDialogFragment.TYPE_GIPHY)
                .show(parentFragmentManager, "giphy_key")
            true
        }
        findPreference<Preference>(KEY_GET_API_KEY)?.setOnPreferenceClickListener {
            openUrl(Constants.GEMINI_KEY_URL)
            true
        }
        findPreference<Preference>(KEY_GET_GIPHY_API_KEY)?.setOnPreferenceClickListener {
            openUrl(Constants.GIPHY_KEY_URL)
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
        findPreference<Preference>(KEY_BACKUP_SETTINGS)?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.launchSettingsBackup()
            true
        }
        findPreference<Preference>(KEY_RESTORE_SETTINGS)?.setOnPreferenceClickListener {
            (activity as? SettingsActivity)?.launchSettingsRestore()
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
        findPreference<Preference>(KEY_GIPHY_API_KEY)?.summary = getString(
            if (secure.giphyApiKey.isBlank()) R.string.ai_key_not_set else R.string.ai_key_set
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

    private fun configureAppLockPreference() {
        val lockPreference = findPreference<SwitchPreferenceCompat>(Prefs.APP_LOCK) ?: return
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val available = BiometricManager.from(requireContext()).canAuthenticate(authenticators) ==
            BiometricManager.BIOMETRIC_SUCCESS
        if (!available) {
            lockPreference.isEnabled = false
            lockPreference.summary = getString(R.string.pref_app_lock_unavailable)
        }
    }

    /** Filters the currently open preference screen by its visible titles and summaries. */
    fun filterPreferences(query: String) {
        val screen = preferenceScreen ?: return
        val emptyRow = searchEmptyPreference ?: Preference(requireContext()).apply {
            title = getString(R.string.settings_search_no_results)
            isSelectable = false
            isEnabled = false
            isVisible = false
        }.also {
            searchEmptyPreference = it
            screen.addPreference(it)
        }
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            setPreferenceTreeVisibility(screen, true, emptyRow)
            emptyRow.isVisible = false
            return
        }
        val hasMatches = filterPreferenceGroup(screen, normalized, emptyRow)
        emptyRow.isVisible = !hasMatches
    }

    private fun filterPreferenceGroup(
        group: PreferenceGroup,
        query: String,
        excluded: Preference
    ): Boolean {
        if (group === excluded) return false
        if (matchesPreference(group, query)) {
            setPreferenceTreeVisibility(group, true, excluded)
            return true
        }
        var anyVisible = false
        for (index in 0 until group.preferenceCount) {
            val child = group.getPreference(index)
            if (child === excluded) continue
            val matches = if (child is PreferenceGroup) {
                filterPreferenceGroup(child, query, excluded)
            } else {
                matchesPreference(child, query)
            }
            child.isVisible = matches
            anyVisible = anyVisible || matches
        }
        return anyVisible
    }

    private fun setPreferenceTreeVisibility(
        preference: Preference,
        visible: Boolean,
        excluded: Preference
    ) {
        if (preference === excluded) return
        preference.isVisible = visible
        if (preference is PreferenceGroup) {
            for (index in 0 until preference.preferenceCount) {
                setPreferenceTreeVisibility(preference.getPreference(index), visible, excluded)
            }
        }
    }

    private fun matchesPreference(preference: Preference, query: String): Boolean =
        preference.title?.toString()?.contains(query, ignoreCase = true) == true ||
            preference.summary?.toString()?.contains(query, ignoreCase = true) == true

    private fun showTextShortcutEditor() {
        val context = requireContext()
        val editor = EditText(context).apply {
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            isSingleLine = false
            hint = getString(R.string.shortcut_editor_hint)
            setText(PreferencesManager.getInstance(context).shortcuts)
        }
        val horizontalPadding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(context).apply {
            setPadding(horizontalPadding, 0, horizontalPadding, 0)
            addView(
                editor,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.pref_shortcuts_title)
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                PreferencesManager.getInstance(context).shortcuts = editor.text.toString()
            }
            .show()
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
        const val KEY_EDIT_SHORTCUTS = "action_edit_shortcuts"
        const val KEY_GIPHY_API_KEY = "action_giphy_key"
        const val KEY_GET_API_KEY = "action_get_api_key"
        const val KEY_GET_GIPHY_API_KEY = "action_get_giphy_key"
        const val KEY_CLEAR_CLIPBOARD = "action_clear_clipboard"
        const val KEY_CLEAR_LEARNED = "action_clear_learned"
        const val KEY_RESET_SETTINGS = "action_reset_settings"
        const val KEY_BACKUP_SETTINGS = "action_backup_settings"
        const val KEY_RESTORE_SETTINGS = "action_restore_settings"
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
