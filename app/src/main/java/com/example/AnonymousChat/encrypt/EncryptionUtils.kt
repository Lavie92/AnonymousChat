package com.example.AnonymousChat.encrypt
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class EncryptionUtils {

    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

        fun generateKey(): Key {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            return keyGenerator.generateKey()
        }

        fun encrypt(text: String, key: Key): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(text.toByteArray())
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        }

        fun decrypt(encryptedText: String, key: Key): String {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes)
        }

        fun getKeyFromString(keyString: String): Key {
            val keyBytes = Base64.decode(keyString, Base64.DEFAULT)
            return SecretKeySpec(keyBytes, ALGORITHM)
        }

        fun getKeyAsString(key: Key): String {
            return Base64.encodeToString(key.encoded, Base64.DEFAULT)
        }
    }
}
