package com.danielgarcia.expensector.feature.onboarding

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.ErrorText
import com.danielgarcia.expensector.core.ui.FoundationScreen
import com.danielgarcia.expensector.core.ui.LoadingButton
import com.danielgarcia.expensector.core.ui.PinTextField
import com.danielgarcia.expensector.security.BiometricAvailability

@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LaunchedEffect(state.step) {
        if (state.step == OnboardingStep.Complete) onCompleted()
    }

    FoundationScreen(title = stringResource(R.string.app_name)) {
        when (state.step) {
            OnboardingStep.Welcome -> {
                Text(stringResource(R.string.welcome_message))
                Button(onClick = viewModel::next) { Text(stringResource(R.string.get_started)) }
            }
            OnboardingStep.Profile -> {
                Text(stringResource(R.string.profile_intro))
                OutlinedTextField(
                    value = state.displayName,
                    onValueChange = viewModel::updateDisplayName,
                    label = { Text(stringResource(R.string.display_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::updateEmail,
                    label = { Text(stringResource(R.string.email_optional)) },
                    singleLine = true,
                )
                ErrorFor(state.error)
                Button(onClick = viewModel::next) { Text(stringResource(R.string.continue_label)) }
            }
            OnboardingStep.Currency -> {
                Text(stringResource(R.string.currency_intro))
                Text(stringResource(R.string.default_currency_mxn))
                Button(onClick = viewModel::next) { Text(stringResource(R.string.confirm_currency)) }
            }
            OnboardingStep.Pin -> {
                Text(stringResource(R.string.create_pin_intro))
                PinTextField(
                    value = state.pin,
                    onValueChange = viewModel::updatePin,
                    label = stringResource(R.string.six_digit_pin),
                )
                ErrorFor(state.error)
                Button(onClick = viewModel::next) { Text(stringResource(R.string.continue_label)) }
            }
            OnboardingStep.ConfirmPin -> {
                Text(stringResource(R.string.confirm_pin_intro))
                PinTextField(
                    value = state.confirmPin,
                    onValueChange = viewModel::updateConfirmPin,
                    label = stringResource(R.string.confirm_pin),
                )
                ErrorFor(state.error)
                Button(onClick = viewModel::next) { Text(stringResource(R.string.continue_label)) }
            }
            OnboardingStep.Biometrics -> {
                Text(stringResource(R.string.biometric_offer))
                Text(biometricAvailabilityText(state.biometricAvailability))
                ErrorFor(state.error)
                Row {
                    LoadingButton(
                        text = stringResource(R.string.enable_biometrics),
                        loading = state.loading,
                        onClick = viewModel::enableBiometrics,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(onClick = viewModel::skipBiometrics) {
                        Text(stringResource(R.string.skip))
                    }
                }
                if (state.biometricEnabled) {
                    Button(onClick = viewModel::next) { Text(stringResource(R.string.finish_setup)) }
                }
            }
            OnboardingStep.Complete -> Text(stringResource(R.string.setup_complete))
        }
    }
}

@Composable
private fun ErrorFor(error: OnboardingError?) {
    val message = when (error) {
        OnboardingError.DisplayNameRequired -> stringResource(R.string.error_display_name_required)
        OnboardingError.InvalidEmail -> stringResource(R.string.error_invalid_email)
        OnboardingError.InvalidPin -> stringResource(R.string.error_invalid_pin)
        OnboardingError.PinMismatch -> stringResource(R.string.error_pin_mismatch)
        OnboardingError.BiometricUnavailable -> stringResource(R.string.error_biometric_unavailable)
        OnboardingError.StorageFailure -> stringResource(R.string.error_storage_failure)
        null -> null
    }
    if (message != null) ErrorText(message)
}

@Composable
private fun biometricAvailabilityText(availability: BiometricAvailability): String =
    when (availability) {
        BiometricAvailability.Available -> stringResource(R.string.biometric_available)
        BiometricAvailability.NoHardware -> stringResource(R.string.biometric_no_hardware)
        BiometricAvailability.HardwareUnavailable -> stringResource(R.string.biometric_hardware_unavailable)
        BiometricAvailability.NoneEnrolled -> stringResource(R.string.biometric_none_enrolled)
        BiometricAvailability.Unsupported -> stringResource(R.string.biometric_unsupported)
        BiometricAvailability.Unknown -> stringResource(R.string.biometric_unknown)
    }
