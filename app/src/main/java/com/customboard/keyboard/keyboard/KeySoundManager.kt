package com.customboard.keyboard.keyboard

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.customboard.keyboard.keyboard.model.Key
import com.customboard.keyboard.keyboard.model.KeyType
import com.customboard.keyboard.settings.PreferencesManager
import com.customboard.keyboard.utils.KeyCodes

/**
 * Key click sounds and haptics.
 *
 * Sounds come from the platform sound pool ([AudioManager.playSoundEffect]) so no audio files
 * have to be shipped - that keeps the APK tiny and automatically respects the user's system
 * keyboard-sound preference. Different key classes use different effects.
 */
class KeySoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = PreferencesManager.getInstance(appContext)
    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun playFor(key: Key, view: View?) {
        playSound(key)
        vibrate(key, view)
    }

    fun playSound(key: Key) {
        if (!prefs.soundEnabled) return
        val manager = audioManager ?: return
        if (prefs.respectSilentMode && manager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

        val effect = when {
            key.type == KeyType.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            key.type == KeyType.DELETE -> AudioManager.FX_KEYPRESS_DELETE
            key.type == KeyType.ENTER -> AudioManager.FX_KEYPRESS_RETURN
            key.code == KeyCodes.SHIFT -> AudioManager.FX_KEYPRESS_STANDARD
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }
        val volume = volumeForProfile()
        runCatching { manager.playSoundEffect(effect, volume) }
    }

    private fun volumeForProfile(): Float {
        val base = prefs.soundVolume / 100f
        val profileScale = when (prefs.soundProfile) {
            "soft" -> 0.5f
            "mechanical" -> 1f
            "typewriter" -> 0.9f
            "bubble" -> 0.7f
            else -> 0.8f
        }
        return (base * profileScale).coerceIn(0f, 1f)
    }

    fun vibrate(key: Key, view: View?) {
        if (!prefs.hapticEnabled) return
        val duration = when (key.type) {
            KeyType.DELETE, KeyType.ENTER, KeyType.SHIFT -> prefs.vibrationStrength + 4
            else -> prefs.vibrationStrength
        }.coerceIn(0, 100)
        if (duration == 0) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val amplitude = ((duration / 80f) * 255).toInt().coerceIn(1, 255)
            runCatching {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(duration.toLong(), amplitude)
                )
            }.onFailure { fallbackHaptic(view) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator?.vibrate(duration.toLong()) }.onFailure { fallbackHaptic(view) }
        }
    }

    private fun fallbackHaptic(view: View?) {
        view?.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /** Longer buzz used for gesture completion, toolbar taps and errors. */
    fun gestureFeedback(view: View? = null) {
        if (!prefs.hapticEnabled) return
        view?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                vibrator?.vibrate(VibrationEffect.createOneShot(28L, 160))
            }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator?.vibrate(28L) }
        }
    }
}
