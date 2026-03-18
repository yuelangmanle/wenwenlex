package com.yueliangmanle.danci.core.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface AiCredentialStore {
    suspend fun saveApiKey(key: String)
    suspend fun readApiKey(): String?
    suspend fun clearApiKey()
}

class AndroidKeystoreAiCredentialStore(
    private val sharedPreferences: SharedPreferences,
) : AiCredentialStore {
    override suspend fun saveApiKey(key: String) {
        if (key.isBlank()) {
            clearApiKey()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(key.toByteArray(StandardCharsets.UTF_8))
        sharedPreferences.edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    override suspend fun readApiKey(): String? {
        val ciphertext = sharedPreferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val iv = sharedPreferences.getString(KEY_IV, null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
        )
        val decrypted = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
        return decrypted.toString(StandardCharsets.UTF_8).takeIf(String::isNotBlank)
    }

    override suspend fun clearApiKey() {
        sharedPreferences.edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return keyGenerator.generateKey()
    }
}

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "wenwenlex_ai_api_key"
private const val KEY_CIPHERTEXT = "ai_key_ciphertext"
private const val KEY_IV = "ai_key_iv"
private const val GCM_TAG_LENGTH_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"

private object AiCredentialStoreHolder {
    @Volatile
    var instance: AiCredentialStore? = null
}

fun buildAiCredentialStore(context: Context): AiCredentialStore {
    AiCredentialStoreHolder.instance?.let { return it }
    return synchronized(AiCredentialStoreHolder) {
        AiCredentialStoreHolder.instance ?: AndroidKeystoreAiCredentialStore(
            sharedPreferences = context.applicationContext.getSharedPreferences(
                "wenwenlex_ai_credentials",
                Context.MODE_PRIVATE,
            ),
        ).also { store ->
            AiCredentialStoreHolder.instance = store
        }
    }
}
