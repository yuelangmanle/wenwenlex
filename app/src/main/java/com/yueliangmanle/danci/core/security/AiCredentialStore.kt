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
    suspend fun saveApiKey(profileId: String, key: String)
    suspend fun readApiKey(profileId: String): String?
    suspend fun clearApiKey(profileId: String)
}

class AndroidKeystoreAiCredentialStore(
    private val sharedPreferences: SharedPreferences,
) : AiCredentialStore {
    override suspend fun saveApiKey(key: String) {
        saveApiKey(LEGACY_PROFILE_ID, key)
    }

    override suspend fun readApiKey(): String? = readApiKey(LEGACY_PROFILE_ID)

    override suspend fun clearApiKey() {
        clearApiKey(LEGACY_PROFILE_ID)
    }

    override suspend fun saveApiKey(profileId: String, key: String) {
        if (key.isBlank()) {
            clearApiKey(profileId)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(profileId))
        val encrypted = cipher.doFinal(key.toByteArray(StandardCharsets.UTF_8))
        sharedPreferences.edit()
            .putString(ciphertextKey(profileId), Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(ivKey(profileId), Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    override suspend fun readApiKey(profileId: String): String? {
        val ciphertext = sharedPreferences.getString(ciphertextKey(profileId), null) ?: return null
        val iv = sharedPreferences.getString(ivKey(profileId), null) ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(profileId),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
        )
        val decrypted = cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP))
        return decrypted.toString(StandardCharsets.UTF_8).takeIf(String::isNotBlank)
    }

    override suspend fun clearApiKey(profileId: String) {
        sharedPreferences.edit()
            .remove(ciphertextKey(profileId))
            .remove(ivKey(profileId))
            .apply()
    }

    private fun secretKey(profileId: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val keyAlias = keyAlias(profileId)
        val existing = keyStore.getKey(keyAlias, null) as? SecretKey
        if (existing != null) {
            return existing
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
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
private const val KEY_ALIAS_PREFIX = "wenwenlex_ai_api_key_"
private const val KEY_CIPHERTEXT_PREFIX = "ai_key_ciphertext_"
private const val KEY_IV_PREFIX = "ai_key_iv_"
private const val GCM_TAG_LENGTH_BITS = 128
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val LEGACY_PROFILE_ID = "legacy-default"

private fun keyAlias(profileId: String): String = KEY_ALIAS_PREFIX + profileId

private fun ciphertextKey(profileId: String): String = KEY_CIPHERTEXT_PREFIX + profileId

private fun ivKey(profileId: String): String = KEY_IV_PREFIX + profileId

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
