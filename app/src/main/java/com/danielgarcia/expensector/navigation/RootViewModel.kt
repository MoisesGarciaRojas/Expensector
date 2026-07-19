package com.danielgarcia.expensector.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.security.PinSecurityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RootDestination {
    Loading,
    Onboarding,
    Locked,
    Home,
}

data class RootUiState(
    val destination: RootDestination = RootDestination.Loading,
    val profile: LocalOwnerProfile? = null,
    val preferences: AppPreferences = AppPreferences(
        onboardingCompleted = false,
        biometricEnabled = false,
        lockDuration = LockDuration.Default,
        lastBackgroundAtEpochMillis = null,
    ),
)

@HiltViewModel
class RootViewModel @Inject constructor(
    profileRepository: LocalOwnerProfileRepository,
    preferencesRepository: AppPreferencesRepository,
    private val pinSecurityRepository: PinSecurityRepository,
) : ViewModel() {
    private val hasPin = MutableStateFlow<Boolean?>(null)

    val state: StateFlow<RootUiState> = combine(
        profileRepository.observeProfile(),
        preferencesRepository.observePreferences(),
        hasPin,
    ) { profile, preferences, pinExists ->
        val destination = when {
            pinExists == null -> RootDestination.Loading
            !preferences.onboardingCompleted || profile == null || !pinExists -> RootDestination.Onboarding
            else -> RootDestination.Locked
        }
        RootUiState(destination = destination, profile = profile, preferences = preferences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState())

    init {
        viewModelScope.launch {
            preferencesRepository.observePreferences().collect {
                hasPin.value = pinSecurityRepository.hasPin()
            }
        }
    }
}
