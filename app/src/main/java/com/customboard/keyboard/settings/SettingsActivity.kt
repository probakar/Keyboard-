package com.customboard.keyboard.settings

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivitySettingsBinding

/** Hosts every settings screen. */
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val section = intent?.getStringExtra(EXTRA_SECTION)
            val fragment = SettingsFragment.forSection(section)
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, fragment)
                .commit()
            title = getString(titleFor(section))
        }

        supportFragmentManager.addOnBackStackChangedListener {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: androidx.preference.Preference
    ): Boolean {
        val fragmentName = pref.fragment ?: return false
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader, fragmentName
        )
        fragment.arguments = pref.extras
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                title = getString(R.string.app_settings)
            } else {
                finish()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun titleFor(section: String?): Int = when (section) {
        SECTION_THEME -> R.string.settings_theme
        SECTION_AI -> R.string.settings_ai
        SECTION_CLIPBOARD -> R.string.settings_clipboard
        SECTION_LAYOUT -> R.string.settings_layout
        SECTION_TYPING -> R.string.settings_typing
        SECTION_LANGUAGES -> R.string.settings_languages
        else -> R.string.app_settings
    }

    companion object {
        const val EXTRA_SECTION = "extra_section"
        const val SECTION_THEME = "theme"
        const val SECTION_AI = "ai"
        const val SECTION_CLIPBOARD = "clipboard"
        const val SECTION_LAYOUT = "layout"
        const val SECTION_TYPING = "typing"
        const val SECTION_LANGUAGES = "languages"
    }
}
