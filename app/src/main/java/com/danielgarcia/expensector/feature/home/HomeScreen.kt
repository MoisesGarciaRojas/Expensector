package com.danielgarcia.expensector.feature.home

import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.FoundationScreen

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenFinancialSetup: () -> Unit,
    onManualLock: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val profile = state.profile
    FoundationScreen(title = stringResource(R.string.home_title)) {
        Text(stringResource(R.string.home_foundation_message))
        Text(stringResource(R.string.owner_label, profile?.displayName.orEmpty()))
        Text(stringResource(R.string.currency_label, profile?.defaultCurrencyCode ?: "MXN"))
        state.summary?.let { summary ->
            Text(stringResource(R.string.default_space_label, summary.defaultSpace?.displayName ?: "Personal", summary.defaultSpace?.defaultCurrencyCode ?: "MXN"))
            summary.currentPeriod?.let {
                Text(stringResource(R.string.current_period_label, it.startDate.toString(), it.endDate.toString(), it.adjustedExpectedPaymentDate.toString()))
            }
            Text(stringResource(R.string.home_stage2_counts, summary.activeAccountCount, summary.categoryCount))
            Text(stringResource(R.string.home_opening_totals, summary.openingAssetMinor / 100.0, summary.openingCreditCardOutstandingMinor / 100.0))
        }
        Button(onClick = onOpenFinancialSetup) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.financial_setup_title))
            Text(stringResource(R.string.financial_setup_title))
        }
        Button(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            Text(stringResource(R.string.settings))
        }
        OutlinedButton(onClick = onManualLock) {
            Icon(Icons.Outlined.Lock, contentDescription = stringResource(R.string.manual_lock))
            Text(stringResource(R.string.manual_lock))
        }
    }
}
