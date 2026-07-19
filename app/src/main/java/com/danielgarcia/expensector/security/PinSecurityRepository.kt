package com.danielgarcia.expensector.security

interface PinSecurityRepository {
    suspend fun hasPin(): Boolean
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): PinVerificationResult
    suspend fun changePin(currentPin: String, newPin: String): PinVerificationResult
}

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data object InvalidPin : PinVerificationResult
    data class Cooldown(val remainingMillis: Long) : PinVerificationResult
}
