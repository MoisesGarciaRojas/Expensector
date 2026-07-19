package com.danielgarcia.expensector.feature.authentication

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.ErrorText
import com.danielgarcia.expensector.core.ui.FoundationScreen
import com.danielgarcia.expensector.core.ui.LoadingButton
import com.danielgarcia.expensector.core.ui.PinTextField
import com.danielgarcia.expensector.security.BiometricAuthResult
import com.danielgarcia.expensector.security.BiometricAvailability
import com.danielgarcia.expensector.security.BiometricPromptCoordinator

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    biometricAvailability: BiometricAvailability,
    biometricPromptCoordinator: BiometricPromptCoordinator,
    onAuthenticated: () -> Unit,
    viewModel: AuthenticationViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val shouldPrompt = remember(biometricEnabled, biometricAvailability) {
        biometricEnabled && biometricAvailability == BiometricAvailability.Available
    }
    LaunchedEffect(shouldPrompt) {
        if (shouldPrompt) {
            biometricPromptCoordinator.authenticate { result ->
                if (result == BiometricAuthResult.Success) onAuthenticated()
            }
        }
    }

    FoundationScreen(title = stringResource(R.string.locked_title)) {
        Text(stringResource(R.string.locked_message))
        PinTextField(
            value = state.pin,
            onValueChange = viewModel::updatePin,
            label = stringResource(R.string.six_digit_pin),
        )
        when (val error = state.error) {
            AuthenticationError.InvalidPin -> ErrorText(stringResource(R.string.error_invalid_pin_try_again))
            is AuthenticationError.Cooldown -> ErrorText(stringResource(R.string.error_pin_cooldown, error.remainingMillis / 1000))
            AuthenticationError.Unexpected -> ErrorText(stringResource(R.string.error_unexpected))
            null -> Unit
        }
        LoadingButton(
            text = stringResource(R.string.unlock),
            loading = state.loading,
            onClick = { viewModel.unlock(onAuthenticated) },
        )
        if (shouldPrompt) {
            OutlinedButton(
                onClick = {
                    biometricPromptCoordinator.authenticate { result ->
                        if (result == BiometricAuthResult.Success) onAuthenticated()
                    }
                },
            ) {
                Text(stringResource(R.string.use_biometric_unlock))
            }
        }
    }
}
