package com.danielgarcia.expensector.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

class Pbkdf2PinVerifierService @Inject constructor() {
    companion object {
        const val Algorithm = "PBKDF2WithHmacSHA256"
        const val Iterations = 210_000
        private const val SaltBytes = 16
        private const val KeyBits = 256
    }

    private val random = SecureRandom()

    fun createVerifier(pin: String): PinVerifier {
        require(PinPolicy.isValid(pin))
        val salt = ByteArray(SaltBytes).also(random::nextBytes)
        val verifier = derive(pin, salt, Iterations)
        return PinVerifier(
            algorithm = Algorithm,
            iterations = Iterations,
            saltBase64 = salt.toBase64(),
            verifierBase64 = verifier.toBase64(),
        )
    }

    fun verify(pin: String, verifier: PinVerifier): Boolean {
        if (!PinPolicy.isValid(pin)) return false
        if (verifier.algorithm != Algorithm) return false
        val salt = verifier.saltBase64.fromBase64()
        val expected = verifier.verifierBase64.fromBase64()
        val actual = derive(pin, salt, verifier.iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KeyBits)
        return try {
            SecretKeyFactory.getInstance(Algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

private fun ByteArray.toBase64(): String =
    Base64.getEncoder().encodeToString(this)

private fun String.fromBase64(): ByteArray =
    Base64.getDecoder().decode(this)
