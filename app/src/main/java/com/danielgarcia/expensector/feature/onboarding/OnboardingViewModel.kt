package com.danielgarcia.expensector.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.core.common.isValidOptionalEmail
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.Stage2Repository
import com.danielgarcia.expensector.platform.Clock
import com.danielgarcia.expensector.security.BiometricAvailability
import com.danielgarcia.expensector.security.BiometricAvailabilityProvider
import com.danielgarcia.expensector.security.PinPolicy
import com.danielgarcia.expensector.security.PinSecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    Welcome,
    Profile,
    Currency,
    Pin,
    ConfirmPin,
    Biometrics,
    Complete,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val displayName: String = "",
    val email: String = "",
    val currencyCode: String = "MXN",
    val pin: String = "",
    val confirmPin: String = "",
    val biometricAvailability: BiometricAvailability = BiometricAvailability.Unknown,
    val biometricEnabled: Boolean = false,
    val loading: Boolean = false,
    val error: OnboardingError? = null,
)

enum class OnboardingError {
    DisplayNameRequired,
    InvalidEmail,
    InvalidPin,
    PinMismatch,
    BiometricUnavailable,
    StorageFailure,
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: LocalOwnerProfileRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val stage2Repository: Stage2Repository,
    private val pinSecurityRepository: PinSecurityRepository,
    private val biometricAvailabilityChecker: BiometricAvailabilityProvider,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun updateDisplayName(value: String) = _state.update { it.copy(displayName = value, error = null) }
    fun updateEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun updatePin(value: String) = _state.update { it.copy(pin = value.filter(Char::isDigit).take(6), error = null) }
    fun updateConfirmPin(value: String) = _state.update { it.copy(confirmPin = value.filter(Char::isDigit).take(6), error = null) }

    fun next() {
        when (_state.value.step) {
            OnboardingStep.Welcome -> _state.update { it.copy(step = OnboardingStep.Profile, error = null) }
            OnboardingStep.Profile -> validateProfile()
            OnboardingStep.Currency -> _state.update { it.copy(step = OnboardingStep.Pin, error = null) }
            OnboardingStep.Pin -> validatePin()
            OnboardingStep.ConfirmPin -> validateConfirmation()
            OnboardingStep.Biometrics -> finish()
            OnboardingStep.Complete -> Unit
        }
    }

    fun enableBiometrics() {
        val availability = biometricAvailabilityChecker.check()
        if (availability == BiometricAvailability.Available) {
            _state.update {
                it.copy(
                    biometricAvailability = availability,
                    biometricEnabled = true,
                    error = null,
                )
            }
        } else {
            _state.update {
                it.copy(
                    biometricAvailability = availability,
                    biometricEnabled = false,
                    error = OnboardingError.BiometricUnavailable,
                )
            }
        }
    }

    fun skipBiometrics() {
        _state.update { it.copy(biometricEnabled = false, error = null) }
        finish()
    }

    private fun validateProfile() {
        val current = _state.value
        when {
            current.displayName.isBlank() -> _state.update { it.copy(error = OnboardingError.DisplayNameRequired) }
            !isValidOptionalEmail(current.email) -> _state.update { it.copy(error = OnboardingError.InvalidEmail) }
            else -> _state.update { it.copy(step = OnboardingStep.Currency, error = null) }
        }
    }

    private fun validatePin() {
        if (PinPolicy.isValid(_state.value.pin)) {
            _state.update { it.copy(step = OnboardingStep.ConfirmPin, error = null) }
        } else {
            _state.update { it.copy(error = OnboardingError.InvalidPin) }
        }
    }

    private fun validateConfirmation() {
        val current = _state.value
        when {
            !PinPolicy.isValid(current.confirmPin) -> _state.update { it.copy(error = OnboardingError.InvalidPin) }
            current.pin != current.confirmPin -> _state.update { it.copy(confirmPin = "", error = OnboardingError.PinMismatch) }
            else -> _state.update {
                it.copy(
                    step = OnboardingStep.Biometrics,
                    biometricAvailability = biometricAvailabilityChecker.check(),
                    error = null,
                )
            }
        }
    }

    private fun finish() {
        val current = _state.value
        if (!PinPolicy.isValid(current.pin) || current.pin != current.confirmPin) {
            _state.update { it.copy(error = OnboardingError.InvalidPin) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val now = clock.nowEpochMillis()
                val profile = LocalOwnerProfile(
                    id = UUID.randomUUID().toString(),
                    displayName = current.displayName.trim(),
                    email = current.email.trim().takeIf { it.isNotBlank() },
                    defaultCurrencyCode = current.currencyCode,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
                profileRepository.saveProfile(profile)
                stage2Repository.initializeDefaults(
                    displayName = "Personal",
                    currencyCode = current.currencyCode,
                    trackingStartDate = java.time.LocalDate.now(),
                )
                pinSecurityRepository.setPin(current.pin)
                preferencesRepository.setBiometricEnabled(current.biometricEnabled)
                preferencesRepository.setOnboardingCompleted(true)
            }.onSuccess {
                _state.update {
                    it.copy(
                        step = OnboardingStep.Complete,
                        pin = "",
                        confirmPin = "",
                        loading = false,
                    )
                }
            }.onFailure {
                _state.update { it.copy(pin = "", confirmPin = "", loading = false, error = OnboardingError.StorageFailure) }
            }
        }
    }
}
