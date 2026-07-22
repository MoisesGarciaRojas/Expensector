package com.danielgarcia.expensector.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.core.common.isValidOptionalEmail
import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.platform.Clock
import com.danielgarcia.expensector.security.BiometricAvailability
import com.danielgarcia.expensector.security.BiometricAvailabilityProvider
import com.danielgarcia.expensector.security.PinPolicy
import com.danielgarcia.expensector.security.PinSecurityRepository
import com.danielgarcia.expensector.security.PinVerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val profile: LocalOwnerProfile? = null,
    val preferences: AppPreferences = AppPreferences(false, false, LockDuration.Default, null),
    val displayName: String = "",
    val email: String = "",
    val currentPin: String = "",
    val newPin: String = "",
    val biometricAvailability: BiometricAvailability = BiometricAvailability.Unknown,
    val error: SettingsError? = null,
    val successMessage: SettingsSuccess? = null,
)

enum class SettingsError {
    DisplayNameRequired,
    InvalidEmail,
    CurrentPinRequired,
    InvalidPin,
    PinMismatch,
    BiometricUnavailable,
    StorageFailure,
}

enum class SettingsSuccess {
    ProfileSaved,
    SecurityUpdated,
    PinChanged,
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: LocalOwnerProfileRepository,
    private val preferencesRepository: AppPreferencesRepository,
    private val pinSecurityRepository: PinSecurityRepository,
    private val biometricAvailabilityChecker: BiometricAvailabilityProvider,
    private val clock: Clock,
) : ViewModel() {
    private val form = MutableStateFlow(SettingsUiState())

    val state: StateFlow<SettingsUiState> = combine(
        profileRepository.observeProfile(),
        preferencesRepository.observePreferences(),
        form,
    ) { profile, preferences, formState ->
        formState.copy(
            profile = profile,
            preferences = preferences,
            displayName = formState.displayName.ifBlank { profile?.displayName.orEmpty() },
            email = if (formState.email.isBlank()) profile?.email.orEmpty() else formState.email,
            biometricAvailability = biometricAvailabilityChecker.check(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun updateDisplayName(value: String) = form.update { it.copy(displayName = value, error = null, successMessage = null) }
    fun updateEmail(value: String) = form.update { it.copy(email = value, error = null, successMessage = null) }
    fun updateCurrentPin(value: String) = form.update { it.copy(currentPin = value.filter(Char::isDigit).take(6), error = null, successMessage = null) }
    fun updateNewPin(value: String) = form.update { it.copy(newPin = value.filter(Char::isDigit).take(6), error = null, successMessage = null) }

    fun saveProfile() {
        val current = state.value
        val profile = current.profile ?: return
        when {
            current.displayName.isBlank() -> form.update { it.copy(error = SettingsError.DisplayNameRequired) }
            !isValidOptionalEmail(current.email) -> form.update { it.copy(error = SettingsError.InvalidEmail) }
            else -> viewModelScope.launch {
                runCatching {
                    profileRepository.saveProfile(
                        profile.copy(
                            displayName = current.displayName.trim(),
                            email = current.email.trim().takeIf { it.isNotBlank() },
                            updatedAtEpochMillis = clock.nowEpochMillis(),
                        ),
                    )
                }.onSuccess {
                    form.update { it.copy(successMessage = SettingsSuccess.ProfileSaved, error = null) }
                }.onFailure {
                    form.update { it.copy(error = SettingsError.StorageFailure) }
                }
            }
        }
    }

    fun setLockDuration(duration: LockDuration, onSecurityInvalidated: () -> Unit) {
        updateSecurityWithCurrentPin(onSecurityInvalidated) {
            preferencesRepository.setLockDuration(duration)
        }
    }

    fun setBiometricEnabled(enabled: Boolean, onSecurityInvalidated: () -> Unit) {
        if (enabled && biometricAvailabilityChecker.check() != BiometricAvailability.Available) {
            form.update { it.copy(error = SettingsError.BiometricUnavailable) }
            return
        }
        updateSecurityWithCurrentPin(onSecurityInvalidated) {
            preferencesRepository.setBiometricEnabled(enabled)
        }
    }

    fun changePin(confirmPin: String) {
        val current = state.value
        if (!PinPolicy.isValid(current.newPin) || current.newPin != confirmPin) {
            form.update { it.copy(newPin = "", error = SettingsError.PinMismatch, successMessage = null) }
            return
        }
        val currentPin = current.currentPin
        val newPin = current.newPin
        if (!PinPolicy.isValid(currentPin)) {
            form.update { it.copy(error = SettingsError.CurrentPinRequired, successMessage = null) }
            return
        }
        viewModelScope.launch {
            val result = pinSecurityRepository.changePin(currentPin, newPin)
            when (result) {
                PinVerificationResult.Success -> form.update {
                    it.copy(
                        currentPin = "",
                        newPin = "",
                        error = null,
                        successMessage = SettingsSuccess.PinChanged,
                    )
                }
                else -> form.update {
                    it.copy(
                        currentPin = "",
                        newPin = "",
                        error = SettingsError.InvalidPin,
                        successMessage = null,
                    )
                }
            }
        }
    }

    private fun updateSecurityWithCurrentPin(
        onSecurityInvalidated: () -> Unit,
        update: suspend () -> Any?,
    ) {
        val currentPin = state.value.currentPin
        if (!PinPolicy.isValid(currentPin)) {
            form.update { it.copy(error = SettingsError.CurrentPinRequired) }
            return
        }
        viewModelScope.launch {
            val verification = pinSecurityRepository.verifyPin(currentPin)
            if (verification != PinVerificationResult.Success) {
                form.update { it.copy(currentPin = "", newPin = "", error = SettingsError.InvalidPin) }
                return@launch
            }
            runCatching { update() }
                .onSuccess {
                    form.update {
                        it.copy(
                            currentPin = "",
                            newPin = "",
                            error = null,
                            successMessage = SettingsSuccess.SecurityUpdated,
                        )
                    }
                    onSecurityInvalidated()
                }
                .onFailure {
                    form.update { it.copy(currentPin = "", newPin = "", error = SettingsError.StorageFailure) }
                }
        }
    }
}
