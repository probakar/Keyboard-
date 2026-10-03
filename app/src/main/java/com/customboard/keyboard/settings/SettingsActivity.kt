package com.customboard.keyboard.settings

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import com.customboard.keyboard.R
import com.customboard.keyboard.databinding.ActivitySettingsBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** Hosts every settings screen. */
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private lateinit var binding: ActivitySettingsBinding
    var currentSearchQuery: String = ""
        private set

    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(::writeSettingsBackup) }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(::readSettingsBackup) }

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
            applySettingsSearch(currentSearchQuery)
        }

        if (PreferencesManager.getInstance(this).appLock) requireAuthentication()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add(Menu.NONE, MENU_SETTINGS_SEARCH, Menu.NONE, R.string.settings_search_hint)
        item.setIcon(android.R.drawable.ic_menu_search)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
        val searchView = SearchView(this).apply {
            queryHint = getString(R.string.settings_search_hint)
            setIconifiedByDefault(true)
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    currentSearchQuery = query
                    applySettingsSearch(query)
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    currentSearchQuery = newText
                    applySettingsSearch(newText)
                    return true
                }
            })
        }
        item.actionView = searchView
        return true
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

    fun launchSettingsBackup() {
        backupLauncher.launch("customboard-settings.json")
    }

    fun launchSettingsRestore() {
        restoreLauncher.launch(arrayOf("application/json", "text/plain"))
    }

    fun applySettingsSearch(query: String) {
        (supportFragmentManager.findFragmentById(R.id.settings_container) as? SettingsFragment)
            ?.filterPreferences(query)
    }

    private fun writeSettingsBackup(uri: android.net.Uri) {
        runCatching {
            val manager = PreferencesManager.getInstance(this)
            val storedPreferences = JSONObject()
            manager.snapshotForBackup().forEach { (key, value) ->
                encodePreference(value)?.let { storedPreferences.put(key, it) }
            }
            val document = JSONObject()
                .put(BACKUP_FORMAT_KEY, BACKUP_FORMAT)
                .put(BACKUP_VERSION_KEY, BACKUP_VERSION)
                .put(BACKUP_PREFERENCES_KEY, storedPreferences)
            contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(StandardCharsets.UTF_8)?.use {
                it.write(document.toString(2))
            } ?: error("Could not open the destination")
        }.onSuccess {
            val fileName = uri.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() } ?: "settings.json"
            Toast.makeText(
                this,
                getString(R.string.backup_success, fileName),
                Toast.LENGTH_SHORT
            ).show()
        }.onFailure {
            Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodePreference(value: Any?): JSONObject? = when (value) {
        is Boolean -> JSONObject().put("type", "boolean").put("value", value)
        is Int -> JSONObject().put("type", "int").put("value", value)
        is Long -> JSONObject().put("type", "long").put("value", value)
        is Float -> JSONObject().put("type", "float").put("value", value.toDouble())
        is String -> JSONObject().put("type", "string").put("value", value)
        is Set<*> -> JSONObject().put(
            "type", "string_set"
        ).put("value", JSONArray().apply {
            value.filterIsInstance<String>().forEach { put(it) }
        })
        else -> null
    }

    private fun readSettingsBackup(uri: android.net.Uri) {
        runCatching {
            val json = contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BACKUP_BYTES) { "Backup file is too large" }
                    output.write(buffer, 0, count)
                }
                String(output.toByteArray(), StandardCharsets.UTF_8)
            } ?: error("Could not read the backup")

            val document = JSONObject(json)
            require(document.optString(BACKUP_FORMAT_KEY) == BACKUP_FORMAT) {
                getString(R.string.settings_backup_format_error)
            }
            require(document.optInt(BACKUP_VERSION_KEY) == BACKUP_VERSION) {
                getString(R.string.settings_backup_format_error)
            }
            val storedPreferences = document.optJSONObject(BACKUP_PREFERENCES_KEY)
                ?: error(getString(R.string.settings_backup_format_error))
            val values = buildMap {
                val keys = storedPreferences.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val encoded = storedPreferences.optJSONObject(key) ?: continue
                    val value = decodePreference(encoded) ?: continue
                    put(key, value)
                }
            }
            PreferencesManager.getInstance(this).restoreSnapshot(values)
        }.onSuccess {
            Toast.makeText(this, R.string.restore_success, Toast.LENGTH_SHORT).show()
            recreate()
        }.onFailure {
            Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodePreference(encoded: JSONObject): Any? {
        val value = encoded.opt("value")
        return when (encoded.optString("type")) {
            "boolean" -> value as? Boolean
            "int" -> (value as? Number)?.toInt()
            "long" -> (value as? Number)?.toLong()
            "float" -> (value as? Number)?.toFloat()
            "string" -> value as? String
            "string_set" -> (value as? JSONArray)?.let { array ->
                buildSet {
                    for (index in 0 until array.length()) {
                        array.optString(index).takeIf { it.isNotEmpty() }?.let(::add)
                    }
                }
            }
            else -> null
        }
    }

    private fun requireAuthentication() {
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        if (BiometricManager.from(this).canAuthenticate(authenticators) !=
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            Toast.makeText(this, R.string.pref_app_lock_unavailable, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.pref_app_lock_title))
            .setSubtitle(getString(R.string.settings_unlock_subtitle))
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    setNegativeButtonText(getString(android.R.string.cancel))
                }
            }
            .build()
        prompt.authenticate(promptInfo)
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

        private const val MENU_SETTINGS_SEARCH = 1001
        private const val BACKUP_FORMAT_KEY = "format"
        private const val BACKUP_FORMAT = "customboard-settings"
        private const val BACKUP_VERSION_KEY = "version"
        private const val BACKUP_VERSION = 1
        private const val BACKUP_PREFERENCES_KEY = "preferences"
        private const val MAX_BACKUP_BYTES = 512 * 1024
    }
}
