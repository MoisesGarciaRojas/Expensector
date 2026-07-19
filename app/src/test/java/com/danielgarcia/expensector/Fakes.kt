package com.danielgarcia.expensector

import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.platform.Clock
import com.danielgarcia.expensector.security.PinSecurityRepository
import com.danielgarcia.expensector.security.PinVerificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClock(var now: Long = 0L) : Clock {
    override fun nowEpochMillis(): Long = now
}

class FakeProfileRepository : LocalOwnerProfileRepository {
    val profile = MutableStateFlow<LocalOwnerProfile?>(null)
    override fun observeProfile(): Flow<LocalOwnerProfile?> = profile
    override suspend fun saveProfile(profile: LocalOwnerProfile) {
        this.profile.value = profile
    }
}

class FakePreferencesRepository : AppPreferencesRepository {
    val preferences = MutableStateFlow(
        AppPreferences(
            onboardingCompleted = false,
            biometricEnabled = false,
            lockDuration = LockDuration.Default,
            lastBackgroundAtEpochMillis = null,
        ),
    )

    override fun observePreferences(): Flow<AppPreferences> = preferences
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.value = preferences.value.copy(onboardingCompleted = completed)
    }
    override suspend fun setBiometricEnabled(enabled: Boolean) {
        preferences.value = preferences.value.copy(biometricEnabled = enabled)
    }
    override suspend fun setLockDuration(duration: LockDuration) {
        preferences.value = preferences.value.copy(lockDuration = duration)
    }
    override suspend fun setLastBackgroundAt(timestampMillis: Long?) {
        preferences.value = preferences.value.copy(lastBackgroundAtEpochMillis = timestampMillis)
    }
}

class FakePinSecurityRepository : PinSecurityRepository {
    var pin: String? = null
    var verifyResult: PinVerificationResult? = null

    override suspend fun hasPin(): Boolean = pin != null
    override suspend fun setPin(pin: String) {
        this.pin = pin
    }
    override suspend fun verifyPin(pin: String): PinVerificationResult =
        verifyResult ?: if (pin == this.pin) PinVerificationResult.Success else PinVerificationResult.InvalidPin

    override suspend fun changePin(currentPin: String, newPin: String): PinVerificationResult {
        val result = verifyPin(currentPin)
        if (result == PinVerificationResult.Success) pin = newPin
        return result
    }
}
