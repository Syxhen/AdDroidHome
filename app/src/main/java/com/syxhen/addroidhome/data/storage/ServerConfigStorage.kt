package com.syxhen.addroidhome.data.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.syxhen.addroidhome.data.model.ServerConfig
import com.syxhen.addroidhome.data.network.AdGuardNetworkFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface ServerConfigStorage {
    fun load(): ServerConfig?
    fun save(config: ServerConfig)
    fun clear()
}

/**
 * Encrypts [ServerConfig] using an Android Keystore AES-256/GCM key and persists
 * the IV + ciphertext in private SharedPreferences.
 */
class EncryptedServerConfigStorage(
    context: Context,
) : ServerConfigStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = AdGuardNetworkFactory.json

    override fun load(): ServerConfig? {
        val ivEncoded = prefs.getString(KEY_IV, null) ?: return null
        val payloadEncoded = prefs.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching {
            val iv = Base64.decode(ivEncoded, Base64.NO_WRAP)
            val encrypted = Base64.decode(payloadEncoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plaintext = cipher.doFinal(encrypted).decodeToString()
            json.decodeFromString<ServerConfig>(plaintext)
        }.onFailure {
            clear()
        }.getOrNull()
    }

    override fun save(config: ServerConfig) {
        val plaintext = json.encodeToString(config).encodeToByteArray()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plaintext)
        prefs.edit {
            putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            putString(KEY_PAYLOAD, Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
    }

    override fun clear() {
        prefs.edit {
            remove(KEY_IV)
            remove(KEY_PAYLOAD)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "addroid_home_server_config_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val PREFS_NAME = "addroid_home_secure_prefs"
        const val KEY_IV = "server_config_iv"
        const val KEY_PAYLOAD = "server_config_payload"
    }
}
