package com.danielgarcia.expensector.feature.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.security.PinSecurityRepository
import com.danielgarcia.expensector.security.PinVerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthenticationUiState(
    val pin: String = "",
    val loading: Boolean = false,
    val error: AuthenticationError? = null,
)

sealed interface AuthenticationError {
    data object InvalidPin : AuthenticationError
    data class Cooldown(val remainingMillis: Long) : AuthenticationError
    data object Unexpected : AuthenticationError
}

@HiltViewModel
class AuthenticationViewModel @Inject constructor(
    private val pinSecurityRepository: PinSecurityRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(AuthenticationUiState())
    val state: StateFlow<AuthenticationUiState> = _state.asStateFlow()

    fun updatePin(value: String) {
        _state.update { it.copy(pin = value.filter(Char::isDigit).take(6), error = null) }
    }

    fun unlock(onSuccess: () -> Unit) {
        val pin = _state.value.pin
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { pinSecurityRepository.verifyPin(pin) }
                .onSuccess { result ->
                    when (result) {
                        PinVerificationResult.Success -> {
                            _state.update { it.copy(pin = "", loading = false, error = null) }
                            onSuccess()
                        }
                        PinVerificationResult.InvalidPin -> _state.update {
                            it.copy(pin = "", loading = false, error = AuthenticationError.InvalidPin)
                        }
                        is PinVerificationResult.Cooldown -> _state.update {
                            it.copy(pin = "", loading = false, error = AuthenticationError.Cooldown(result.remainingMillis))
                        }
                    }
                }
                .onFailure {
                    _state.update { it.copy(pin = "", loading = false, error = AuthenticationError.Unexpected) }
                }
        }
    }
}
