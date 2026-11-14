package com.example.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecureTokenCipher {
  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS = "wude_mart_session_key_v1"
  private const val TRANSFORMATION = "AES/GCM/NoPadding"
  private const val GCM_TAG_BITS = 128

  private fun getOrCreateSecretKey(): SecretKey? {
    return runCatching {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return@runCatching existing

        val keyGenerator =
          KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec =
          KeyGenParameterSpec.Builder(
              KEY_ALIAS,
              KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        keyGenerator.generateKey()
      }
      .getOrNull()
  }

  fun encrypt(plaintext: String): String {
    if (plaintext.isBlank()) return ""
    val key = getOrCreateSecretKey() ?: return ""
    return runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        "$ivB64:$cipherB64"
      }
      .getOrDefault("")
  }

  fun decrypt(payload: String): String {
    if (payload.isBlank() || !payload.contains(":")) return ""
    val key = getOrCreateSecretKey() ?: return ""
    return runCatching {
        val parts = payload.split(":", limit = 2)
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8)
      }
      .getOrDefault("")
  }
}
