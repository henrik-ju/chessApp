package chess.chessGame.model

import android.util.Base64
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

private const val ALGORITHM = "AES"
private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KEY_SIZE_BITS = 256
private const val GCM_IV_LENGTH = 12
private const val GCM_TAG_LENGTH = 16

private const val BASE64_FLAGS = Base64.NO_WRAP

object Crypto {

    fun generateNewSecretKey(): String {
        val keyGen = KeyGenerator.getInstance(ALGORITHM)
        keyGen.init(KEY_SIZE_BITS)
        val secretKey = keyGen.generateKey()
        return Base64.encodeToString(secretKey.encoded, BASE64_FLAGS)
    }

    fun getSecretKey(serializedKey: String?): SecretKey? {
        if (serializedKey == null) return null
        val keyBytes = Base64.decode(serializedKey, BASE64_FLAGS)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(plaintext: String, secretKey: SecretKey): String {
        try {
            val plainBytes = plaintext.toByteArray(Charsets.UTF_8)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val random = SecureRandom()

            val iv = ByteArray(GCM_IV_LENGTH)
            random.nextBytes(iv)

            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

            val cipherText = cipher.doFinal(plainBytes)

            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            return Base64.encodeToString(combined, BASE64_FLAGS)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Encryption failure!", e)
        }
    }
    fun decrypt(encryptedData: String, secretKey: SecretKey): String {
        try {
            val combined = Base64.decode(encryptedData, BASE64_FLAGS)

            if (combined.size < GCM_IV_LENGTH) {
                throw RuntimeException("Encrypted data is too short to contain IV.")
            }

            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)

            val cipherTextSize = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)

            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

            val plainText = cipher.doFinal(cipherText)
            return String(plainText, Charsets.UTF_8)
        } catch (e: GeneralSecurityException) {
            throw RuntimeException("Decryption failure! The message may be corrupted or keys are mismatched.", e)
        }
    }
}