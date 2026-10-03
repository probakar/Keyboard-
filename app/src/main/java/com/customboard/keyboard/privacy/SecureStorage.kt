package com.customboard.keyboard.privacy

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.customboard.keyboard.BuildConfig
import com.customboard.keyboard.utils.Constants
import com.customboard.keyboard.utils.Prefs
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encrypted storage backed by the Android Keystore.
 *
 * Used for the Gemini / Tenor API keys so they are never written to disk in clear text and
 * never leave the device. No third-party dependency is required.
 */
class SecureStorage private constructor(context: Context) {

    private val appContext = context.applicationContext

    private val prefs by lazy {
        appContext.getSharedPreferences(Constants.SECURE_PREFS, Context.MODE_PRIVATE)
    }

    /** Google AI Studio key used by [com.customboard.keyboard.ai.GeminiClient]. */
    var geminiApiKey: String
        get() = get(Prefs.AI_API_KEY) ?: BuildConfig.DEFAULT_GEMINI_API_KEY
        set(value) = put(Prefs.AI_API_KEY, value)

    /** Tenor key used for GIF search. */
    var tenorApiKey: String
        get() = get(Prefs.TENOR_API_KEY) ?: BuildConfig.DEFAULT_TENOR_API_KEY
        set(value) = put(Prefs.TENOR_API_KEY, value)

    fun put(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }
        val encrypted = encrypt(value)
        if (encrypted != null) {
            prefs.edit().putString(key, encrypted).apply()
        }
    }

    fun get(key: String): String? {
        val stored = prefs.getString(key, null) ?: return null
        return decrypt(stored)
    }

    fun contains(key: String): Boolean = !get(key).isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(plainText: String): String? = try {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        Base64.encodeToString(combined, Base64.NO_WRAP)
    } catch (e: Exception) {
        Log.w(TAG, "encrypt failed", e)
        null
    }

    private fun decrypt(encoded: String): String? = try {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        if (combined.size <= IV_LENGTH) {
            null
        } else {
            val iv = combined.copyOfRange(0, IV_LENGTH)
            val cipherText = combined.copyOfRange(IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }
    } catch (e: Exception) {
        Log.w(TAG, "decrypt failed", e)
        null
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setRandomizedEncryptionRequired(true)
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    companion object {
        private const val TAG = "SecureStorage"
        private const val KEY_ALIAS = "customboard_secret_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128

        @Volatile
        private var instance: SecureStorage? = null

        fun getInstance(context: Context): SecureStorage =
            instance ?: synchronized(this) {
                instance ?: SecureStorage(context).also { instance = it }
            }
    }
}
