package com.danielgarcia.expensector.feature.financial

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.FoundationScreen
import com.danielgarcia.expensector.domain.AccountType
import com.danielgarcia.expensector.domain.Category
import com.danielgarcia.expensector.domain.FinancialPurpose
import com.danielgarcia.expensector.domain.MoneyMinor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialSetupScreen(
    onBack: () -> Unit,
    viewModel: FinancialSetupViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var typeMenu by remember { mutableStateOf(false) }
    var purposeMenu by remember { mutableStateOf(false) }
    val assetTotal = state.balances.filter { it.type.name == "ASSET_BALANCE" }.sumOf { it.amountMinor }
    val cardDebt = state.balances.filter { it.type.name == "CREDIT_CARD_OUTSTANDING" }.sumOf { it.amountMinor }

    FoundationScreen(title = stringResource(R.string.financial_setup_title)) {
        Text(stringResource(R.string.financial_setup_overview, state.spaces.count { it.active }, state.periods.size, state.accounts.count { it.active }, state.balances.size, state.categories.count { it.active }))
        state.spaces.firstOrNull { it.isDefault }?.let { Text(stringResource(R.string.default_space_label, it.displayName, it.defaultCurrencyCode)) }
        state.periods.firstOrNull { period -> !java.time.LocalDate.now().isBefore(period.startDate) && !java.time.LocalDate.now().isAfter(period.endDate) }?.let {
            Text(stringResource(R.string.current_period_label, it.startDate.toString(), it.endDate.toString(), it.adjustedExpectedPaymentDate.toString()))
        }

        Text(stringResource(R.string.period_settings_title))
        Text(stringResource(R.string.semimonthly_available))
        Text(stringResource(R.string.period_counts, state.periods.size))
        Button(onClick = viewModel::generateMorePeriods) { Text(stringResource(R.string.generate_future_periods)) }

        Text(stringResource(R.string.accounts_title))
        ExposedDropdownMenuBox(expanded = typeMenu, onExpandedChange = { typeMenu = it }) {
            OutlinedTextField(
                value = state.selectedAccountType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.account_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) },
                modifier = androidx.compose.ui.Modifier.menuAnchor(),
            )
            ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                AccountType.entries.forEach {
                    DropdownMenuItem(text = { Text(it.name) }, onClick = {
                        typeMenu = false
                        viewModel.updateAccountType(it)
                    })
                }
            }
        }
        OutlinedTextField(state.accountName, viewModel::updateAccountName, label = { Text(stringResource(R.string.account_name)) }, singleLine = true)
        OutlinedTextField(state.institution, viewModel::updateInstitution, label = { Text(stringResource(R.string.institution_optional)) }, singleLine = true)
        OutlinedTextField(state.lastFour, viewModel::updateLastFour, label = { Text(stringResource(R.string.last_four_optional)) }, singleLine = true)
        OutlinedTextField(state.openingAmount, viewModel::updateOpeningAmount, label = { Text(if (state.selectedAccountType == AccountType.CREDIT_CARD) stringResource(R.string.initial_outstanding) else stringResource(R.string.opening_balance)) }, singleLine = true)
        if (state.selectedAccountType == AccountType.CREDIT_CARD) {
            OutlinedTextField(state.cardLimit, viewModel::updateCardLimit, label = { Text(stringResource(R.string.credit_limit)) }, singleLine = true)
            Text(stringResource(R.string.card_days_default))
        }
        Button(onClick = viewModel::createAccount) { Text(stringResource(R.string.save_account)) }
        state.accounts.forEach { account ->
            Row {
                Text("${account.displayName} - ${account.accountType} - ${account.currencyCode}${if (account.active) "" else " (archived)"}")
                Spacer(androidx.compose.ui.Modifier.width(8.dp))
                OutlinedButton(onClick = { viewModel.archiveAccount(account, account.active) }) {
                    Text(if (account.active) stringResource(R.string.archive) else stringResource(R.string.restore))
                }
            }
        }

        Text(stringResource(R.string.categories_title))
        ExposedDropdownMenuBox(expanded = purposeMenu, onExpandedChange = { purposeMenu = it }) {
            OutlinedTextField(
                value = state.categoryPurpose.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.purpose)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(purposeMenu) },
                modifier = androidx.compose.ui.Modifier.menuAnchor(),
            )
            ExposedDropdownMenu(expanded = purposeMenu, onDismissRequest = { purposeMenu = false }) {
                FinancialPurpose.entries.forEach {
                    DropdownMenuItem(text = { Text(it.name) }, onClick = {
                        purposeMenu = false
                        viewModel.updateCategoryPurpose(it)
                    })
                }
            }
        }
        OutlinedTextField(state.categoryName, viewModel::updateCategoryName, label = { Text(stringResource(R.string.category_name)) }, singleLine = true)
        Button(onClick = viewModel::createCategory) { Text(stringResource(R.string.save_category)) }
        FinancialPurpose.entries.forEach { purpose ->
            Text(purpose.name)
            state.categories.filter { it.purpose == purpose && it.parentCategoryId == null }.take(8).forEach { category ->
                CategoryManagementItem(
                    category = category,
                    onArchive = { viewModel.archiveCategory(category, category.active) },
                    onMerge = { viewModel.mergeIntoFirstCompatibleArchived(category) },
                )
                state.categories.filter { it.parentCategoryId == category.id }.forEach { child ->
                    CategoryManagementItem(
                        category = child,
                        modifier = Modifier.padding(start = 24.dp),
                        onArchive = { viewModel.archiveCategory(child, child.active) },
                        onMerge = { viewModel.mergeIntoFirstCompatibleArchived(child) },
                    )
                }
            }
        }

        Text(stringResource(R.string.opening_summary_title))
        Text(stringResource(R.string.money_owned, MoneyMinor(assetTotal, "MXN").format()))
        Text(stringResource(R.string.card_debt, MoneyMinor(cardDebt, "MXN").format()))
        Text(stringResource(R.string.net_opening_position, MoneyMinor(assetTotal - cardDebt, "MXN").format()))
        state.statusMessage?.let { Text(it) }
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.back)) }
    }
}

@Composable
private fun CategoryManagementItem(
    category: Category,
    onArchive: () -> Unit,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${category.displayName}${if (category.active) "" else " (archived)"}",
            style = if (category.hierarchyLevel == 0) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onArchive) {
                Text(if (category.active) stringResource(R.string.archive) else stringResource(R.string.restore))
            }
            OutlinedButton(onClick = onMerge) {
                Text(stringResource(R.string.merge))
            }
        }
    }
}
