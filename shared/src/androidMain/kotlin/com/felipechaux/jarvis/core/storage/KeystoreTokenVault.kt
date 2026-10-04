package com.felipechaux.jarvis.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM with a non-exportable Android Keystore key; only the
 * ciphertext (IV ‖ data) is written to SharedPreferences.
 */
class KeystoreTokenVault(context: Context) : TokenVault {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return@withContext null
        runCatching {
            val bytes = Base64.decode(stored, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }.getOrNull() // key wiped (e.g. restored backup): treat as unpaired
    }

    override suspend fun write(token: String?) = withContext(Dispatchers.IO) {
        if (token == null) {
            prefs.edit { remove(KEY_TOKEN) }
            return@withContext
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val sealed = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        prefs.edit { putString(KEY_TOKEN, Base64.encodeToString(sealed, Base64.NO_WRAP)) }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "jarvis_device_token"
        const val PREFS = "jarvis_vault"
        const val KEY_TOKEN = "device_token"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
