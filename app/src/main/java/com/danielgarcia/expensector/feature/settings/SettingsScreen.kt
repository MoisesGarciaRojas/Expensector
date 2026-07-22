package com.danielgarcia.expensector.feature.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.ErrorText
import com.danielgarcia.expensector.core.ui.FoundationScreen
import com.danielgarcia.expensector.core.ui.PinTextField
import com.danielgarcia.expensector.domain.LockDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSecurityInvalidated: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var confirmPin by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    FoundationScreen(title = stringResource(R.string.settings)) {
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
        Text(stringResource(R.string.currency_label, state.profile?.defaultCurrencyCode ?: "MXN"))
        Button(onClick = viewModel::saveProfile) { Text(stringResource(R.string.save_profile)) }

        Text(stringResource(R.string.security_requires_pin))
        PinTextField(
            value = state.currentPin,
            onValueChange = viewModel::updateCurrentPin,
            label = stringResource(R.string.current_pin),
        )
        Row {
            Text(stringResource(R.string.enable_biometrics))
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = state.preferences.biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it, onSecurityInvalidated) },
            )
        }
        Text(stringResource(R.string.automatic_lock_label))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = lockDurationLabel(state.preferences.lockDuration),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.automatic_lock_duration)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                LockDuration.entries.forEach { duration ->
                    DropdownMenuItem(
                        text = { Text(lockDurationLabel(duration)) },
                        onClick = {
                            expanded = false
                            viewModel.setLockDuration(duration, onSecurityInvalidated)
                        },
                    )
                }
            }
        }
        PinTextField(
            value = state.newPin,
            onValueChange = viewModel::updateNewPin,
            label = stringResource(R.string.new_pin),
        )
        PinTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it.filter(Char::isDigit).take(6) },
            label = stringResource(R.string.confirm_pin),
        )
        Button(
            onClick = {
                viewModel.changePin(confirmPin)
                confirmPin = ""
            },
        ) {
            Text(stringResource(R.string.change_pin))
        }
        SettingsErrorText(state.error)
        SettingsSuccessText(state.successMessage)
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun lockDurationLabel(duration: LockDuration): String =
    when (duration) {
        LockDuration.Immediately -> stringResource(R.string.lock_immediately)
        LockDuration.OneMinute -> stringResource(R.string.lock_one_minute)
        LockDuration.FiveMinutes -> stringResource(R.string.lock_five_minutes)
        LockDuration.FifteenMinutes -> stringResource(R.string.lock_fifteen_minutes)
    }

@Composable
private fun SettingsErrorText(error: SettingsError?) {
    val message = when (error) {
        SettingsError.DisplayNameRequired -> stringResource(R.string.error_display_name_required)
        SettingsError.InvalidEmail -> stringResource(R.string.error_invalid_email)
        SettingsError.CurrentPinRequired -> stringResource(R.string.error_current_pin_required)
        SettingsError.InvalidPin -> stringResource(R.string.error_invalid_pin_try_again)
        SettingsError.PinMismatch -> stringResource(R.string.error_pin_mismatch)
        SettingsError.BiometricUnavailable -> stringResource(R.string.error_biometric_unavailable)
        SettingsError.StorageFailure -> stringResource(R.string.error_storage_failure)
        null -> null
    }
    if (message != null) ErrorText(message)
}

@Composable
private fun SettingsSuccessText(success: SettingsSuccess?) {
    val message = when (success) {
        SettingsSuccess.ProfileSaved -> stringResource(R.string.profile_saved)
        SettingsSuccess.SecurityUpdated -> stringResource(R.string.security_updated)
        SettingsSuccess.PinChanged -> stringResource(R.string.pin_changed)
        null -> null
    }
    if (message != null) Text(message)
}
