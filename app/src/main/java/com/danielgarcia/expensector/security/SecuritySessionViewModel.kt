package com.danielgarcia.expensector.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.platform.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SecuritySessionViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val securitySessionManager: SecuritySessionManager,
    private val clock: Clock,
) : ViewModel(), DefaultLifecycleObserver {
    private val _state = MutableStateFlow(SecuritySessionState())
    val state: StateFlow<SecuritySessionState> = _state.asStateFlow()

    private val preferences = preferencesRepository.observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = com.danielgarcia.expensector.domain.AppPreferences(
                onboardingCompleted = false,
                biometricEnabled = false,
                lockDuration = com.danielgarcia.expensector.domain.LockDuration.Default,
                lastBackgroundAtEpochMillis = null,
            ),
        )

    init {
        viewModelScope.launch {
            preferences.collect {
                if (!_state.value.authenticated && !it.onboardingCompleted) {
                    _state.value = securitySessionManager.onProcessStart(onboardingCompleted = false)
                }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val currentPreferences = preferences.value
        if (currentPreferences.onboardingCompleted && securitySessionManager.shouldLockOnForeground(currentPreferences)) {
            _state.value = SecuritySessionState(authenticated = false, locked = true)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        viewModelScope.launch {
            preferencesRepository.setLastBackgroundAt(clock.nowEpochMillis())
        }
    }

    fun markAuthenticated() {
        _state.value = SecuritySessionState(authenticated = true, locked = false)
    }

    fun manualLock() {
        _state.value = SecuritySessionState(authenticated = false, locked = true)
    }
}
