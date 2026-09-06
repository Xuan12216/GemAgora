package com.example.gemagora.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object SecurityCryptoUtils {
    private const val SALT_BYTES = 16
    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically strong 16-byte random salt, Base64-encoded.
     */
    fun generateSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Computes SHA-256 hash of (salt + pin) and returns Base64-encoded string.
     */
    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val hash = md.digest(combined)
        return Base64.getEncoder().encodeToString(hash)
    }

    /**
     * Compares two hash strings in constant time to prevent side-channel timing attacks.
     */
    fun verifyPin(inputPin: String, storedSalt: String, storedHash: String): Boolean {
        val computedHash = hashPin(inputPin, storedSalt)
        val a = computedHash.toByteArray(Charsets.UTF_8)
        val b = storedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(a, b)
    }
}
