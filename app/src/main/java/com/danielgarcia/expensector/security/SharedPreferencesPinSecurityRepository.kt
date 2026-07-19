package com.danielgarcia.expensector.security

import android.content.Context
import com.danielgarcia.expensector.platform.Clock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPreferencesPinSecurityRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val verifierService: Pbkdf2PinVerifierService,
    private val retryController: PinRetryController,
    private val clock: Clock,
) : PinSecurityRepository {
    private val preferences = context.getSharedPreferences("expensector_security", Context.MODE_PRIVATE)

    override suspend fun hasPin(): Boolean =
        preferences.contains(Keys.Verifier)

    override suspend fun setPin(pin: String) {
        val verifier = verifierService.createVerifier(pin)
        preferences.edit()
            .putString(Keys.Algorithm, verifier.algorithm)
            .putInt(Keys.Iterations, verifier.iterations)
            .putString(Keys.Salt, verifier.saltBase64)
            .putString(Keys.Verifier, verifier.verifierBase64)
            .putInt(Keys.FailedAttempts, 0)
            .remove(Keys.CooldownUntil)
            .apply()
    }

    override suspend fun verifyPin(pin: String): PinVerificationResult {
        val state = retryState()
        val now = clock.nowEpochMillis()
        if (state.isInCooldown(now)) {
            return PinVerificationResult.Cooldown((state.cooldownUntilEpochMillis ?: now) - now)
        }
        val verifier = currentVerifier() ?: return PinVerificationResult.InvalidPin
        val valid = verifierService.verify(pin, verifier)
        return if (valid) {
            saveRetryState(retryController.registerSuccess())
            PinVerificationResult.Success
        } else {
            val next = retryController.registerFailure(state)
            saveRetryState(next)
            if (next.isInCooldown(now)) {
                PinVerificationResult.Cooldown((next.cooldownUntilEpochMillis ?: now) - now)
            } else {
                PinVerificationResult.InvalidPin
            }
        }
    }

    override suspend fun changePin(currentPin: String, newPin: String): PinVerificationResult {
        val currentResult = verifyPin(currentPin)
        if (currentResult != PinVerificationResult.Success) return currentResult
        setPin(newPin)
        return PinVerificationResult.Success
    }

    private fun currentVerifier(): PinVerifier? {
        val algorithm = preferences.getString(Keys.Algorithm, null) ?: return null
        val salt = preferences.getString(Keys.Salt, null) ?: return null
        val verifier = preferences.getString(Keys.Verifier, null) ?: return null
        return PinVerifier(
            algorithm = algorithm,
            iterations = preferences.getInt(Keys.Iterations, Pbkdf2PinVerifierService.Iterations),
            saltBase64 = salt,
            verifierBase64 = verifier,
        )
    }

    private fun retryState(): PinRetryState =
        PinRetryState(
            failedAttempts = preferences.getInt(Keys.FailedAttempts, 0),
            cooldownUntilEpochMillis = preferences.takeIf { it.contains(Keys.CooldownUntil) }
                ?.getLong(Keys.CooldownUntil, 0L),
        )

    private fun saveRetryState(state: PinRetryState) {
        preferences.edit()
            .putInt(Keys.FailedAttempts, state.failedAttempts)
            .apply {
                val cooldownUntil = state.cooldownUntilEpochMillis
                if (cooldownUntil == null) remove(Keys.CooldownUntil) else putLong(Keys.CooldownUntil, cooldownUntil)
            }
            .apply()
    }

    private object Keys {
        const val Algorithm = "pin_algorithm"
        const val Iterations = "pin_iterations"
        const val Salt = "pin_salt"
        const val Verifier = "pin_verifier"
        const val FailedAttempts = "pin_failed_attempts"
        const val CooldownUntil = "pin_cooldown_until_epoch_millis"
    }
}
