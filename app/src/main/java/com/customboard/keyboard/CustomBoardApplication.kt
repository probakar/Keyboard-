package com.customboard.keyboard

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.customboard.keyboard.autocorrect.LanguageModelManager
import com.customboard.keyboard.clipboard.ClipboardManager
import com.customboard.keyboard.emoji.EmojiManager
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. It warms up the dictionaries and the emoji table in the background
 * so the keyboard is responsive the very first time it is shown.
 */
class CustomBoardApplication : Application() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val prefs = PreferencesManager.getInstance(this)

        AppCompatDelegate.setDefaultNightMode(
            if (prefs.followSystemTheme) AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
        )

        if (prefs.firstLaunchTime == 0L) {
            prefs.firstLaunchTime = System.currentTimeMillis()
        }
        prefs.launchCount += 1

        ThemeManager.getInstance(this).invalidate()

        scope.launch {
            LanguageModelManager.getInstance(this@CustomBoardApplication).load()
            EmojiManager.getInstance(this@CustomBoardApplication).load()
            ClipboardManager.getInstance(this@CustomBoardApplication).cleanUp()
        }
    }
}
