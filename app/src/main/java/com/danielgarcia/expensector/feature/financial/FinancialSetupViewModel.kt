package com.danielgarcia.expensector.feature.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielgarcia.expensector.domain.AccountType
import com.danielgarcia.expensector.domain.Category
import com.danielgarcia.expensector.domain.CategoryMergeResult
import com.danielgarcia.expensector.domain.CategorySaveResult
import com.danielgarcia.expensector.domain.CreditCardDetails
import com.danielgarcia.expensector.domain.FinancialAccount
import com.danielgarcia.expensector.domain.FinancialPurpose
import com.danielgarcia.expensector.domain.FinancialSpace
import com.danielgarcia.expensector.domain.MoneyMinor
import com.danielgarcia.expensector.domain.OpeningBalance
import com.danielgarcia.expensector.domain.OpeningBalanceType
import com.danielgarcia.expensector.domain.Stage2Repository
import com.danielgarcia.expensector.domain.defaultIncludeInAvailableCash
import com.danielgarcia.expensector.domain.normalizeCatalogName
import com.danielgarcia.expensector.platform.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinancialSetupUiState(
    val spaces: List<FinancialSpace> = emptyList(),
    val periods: List<com.danielgarcia.expensector.domain.BudgetPeriod> = emptyList(),
    val accounts: List<FinancialAccount> = emptyList(),
    val cards: List<CreditCardDetails> = emptyList(),
    val balances: List<OpeningBalance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountType: AccountType = AccountType.CHECKING,
    val accountName: String = "",
    val institution: String = "",
    val lastFour: String = "",
    val openingAmount: String = "0.00",
    val cardLimit: String = "0.00",
    val categoryName: String = "",
    val categoryPurpose: FinancialPurpose = FinancialPurpose.NEED,
    val statusMessage: String? = null,
)

@HiltViewModel
class FinancialSetupViewModel @Inject constructor(
    private val repository: Stage2Repository,
    private val clock: Clock,
) : ViewModel() {
    private val form = MutableStateFlow(FinancialSetupUiState())

    val state: StateFlow<FinancialSetupUiState> = combine(
        repository.observeFinancialSpaces(),
        repository.observeBudgetPeriods(),
        repository.observeAccounts(),
        repository.observeCreditCardDetails(),
        repository.observeOpeningBalances(),
        repository.observeCategories(),
    ) { values ->
        FinancialData(
            spaces = values[0] as List<com.danielgarcia.expensector.domain.FinancialSpace>,
            periods = values[1] as List<com.danielgarcia.expensector.domain.BudgetPeriod>,
            accounts = values[2] as List<FinancialAccount>,
            cards = values[3] as List<CreditCardDetails>,
            balances = values[4] as List<OpeningBalance>,
            categories = values[5] as List<Category>,
        )
    }.combine(form) { data, formState ->
        formState.copy(
            spaces = data.spaces,
            periods = data.periods,
            accounts = data.accounts,
            cards = data.cards,
            balances = data.balances,
            categories = data.categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinancialSetupUiState())

    private data class FinancialData(
        val spaces: List<FinancialSpace>,
        val periods: List<com.danielgarcia.expensector.domain.BudgetPeriod>,
        val accounts: List<FinancialAccount>,
        val cards: List<CreditCardDetails>,
        val balances: List<OpeningBalance>,
        val categories: List<Category>,
    )

    init {
        viewModelScope.launch {
            repository.initializeDefaults(trackingStartDate = LocalDate.now())
        }
    }

    fun updateAccountType(type: AccountType) = form.update { it.copy(selectedAccountType = type, statusMessage = null) }
    fun updateAccountName(value: String) = form.update { it.copy(accountName = value, statusMessage = null) }
    fun updateInstitution(value: String) = form.update { it.copy(institution = value, statusMessage = null) }
    fun updateLastFour(value: String) = form.update { it.copy(lastFour = value.filter(Char::isDigit).take(4), statusMessage = null) }
    fun updateOpeningAmount(value: String) = form.update { it.copy(openingAmount = value, statusMessage = null) }
    fun updateCardLimit(value: String) = form.update { it.copy(cardLimit = value, statusMessage = null) }
    fun updateCategoryName(value: String) = form.update { it.copy(categoryName = value, statusMessage = null) }
    fun updateCategoryPurpose(value: FinancialPurpose) = form.update { it.copy(categoryPurpose = value, statusMessage = null) }

    fun createAccount() {
        val current = state.value
        val space = current.spaces.firstOrNull { it.isDefault && it.active } ?: return
        viewModelScope.launch {
            runCatching {
                val now = clock.nowEpochMillis()
                val id = UUID.randomUUID().toString()
                val amount = MoneyMinor.parse(current.openingAmount, space.defaultCurrencyCode)
                val account = FinancialAccount(
                    id = id,
                    financialSpaceId = space.id,
                    accountType = current.selectedAccountType,
                    displayName = current.accountName.trim(),
                    institutionName = current.institution.trim().takeIf { it.isNotBlank() },
                    alias = null,
                    currencyCode = space.defaultCurrencyCode,
                    lastFourDigits = current.lastFour.takeIf { it.isNotBlank() },
                    displayOrder = current.accounts.size,
                    active = true,
                    includeInAvailableCash = defaultIncludeInAvailableCash(current.selectedAccountType),
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                    archivedAtEpochMillis = null,
                )
                val balance = OpeningBalance(
                    id = UUID.randomUUID().toString(),
                    financialAccountId = id,
                    financialSpaceId = space.id,
                    amountMinor = amount.minorUnits,
                    currencyCode = amount.currencyCode,
                    effectiveDate = LocalDate.now(),
                    type = if (current.selectedAccountType == AccountType.CREDIT_CARD) OpeningBalanceType.CREDIT_CARD_OUTSTANDING else OpeningBalanceType.ASSET_BALANCE,
                    note = if (current.selectedAccountType == AccountType.CREDIT_CARD) "Outstanding card balance at setup." else "Balance when I started using Expensector.",
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                )
                val card = if (current.selectedAccountType == AccountType.CREDIT_CARD) {
                    CreditCardDetails(
                        financialAccountId = id,
                        issuer = current.institution.trim().ifBlank { current.accountName.trim() },
                        productName = null,
                        creditLimitMinor = MoneyMinor.parse(current.cardLimit, space.defaultCurrencyCode).minorUnits,
                        currencyCode = space.defaultCurrencyCode,
                        statementClosingDay = 27,
                        paymentDueDay = 12,
                        annualInterestRateBasisPoints = null,
                        active = true,
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now,
                    )
                } else {
                    null
                }
                repository.saveAccount(account, balance, card)
            }.onSuccess {
                form.update { it.copy(accountName = "", institution = "", lastFour = "", openingAmount = "0.00", cardLimit = "0.00", statusMessage = "Account saved.") }
            }.onFailure { error ->
                form.update { it.copy(statusMessage = error.message ?: "Account could not be saved.") }
            }
        }
    }

    fun archiveAccount(account: FinancialAccount, archived: Boolean) {
        viewModelScope.launch { repository.setAccountArchived(account.id, archived) }
    }

    fun createCategory() {
        val current = state.value
        val space = current.spaces.firstOrNull { it.isDefault && it.active } ?: return
        viewModelScope.launch {
            val now = clock.nowEpochMillis()
            val category = Category(
                id = UUID.randomUUID().toString(),
                financialSpaceId = space.id,
                purpose = current.categoryPurpose,
                displayName = current.categoryName.trim(),
                normalizedName = normalizeCatalogName(current.categoryName),
                description = null,
                parentCategoryId = null,
                hierarchyLevel = 0,
                displayOrder = current.categories.size,
                active = true,
                systemProvided = false,
                mergedIntoCategoryId = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                archivedAtEpochMillis = null,
            )
            when (val result = repository.saveCategory(category)) {
                is CategorySaveResult.Saved -> form.update { it.copy(categoryName = "", statusMessage = "Category saved.") }
                is CategorySaveResult.Duplicate -> form.update { it.copy(statusMessage = "Similar category exists: ${result.existing.displayName}. Rename it or create intentionally later.") }
            }
        }
    }

    fun archiveCategory(category: Category, archived: Boolean) {
        viewModelScope.launch { repository.setCategoryArchived(category.id, archived) }
    }

    fun mergeIntoFirstCompatibleArchived(category: Category) {
        val destination = state.value.categories.firstOrNull {
            it.id != category.id && it.purpose == category.purpose && it.hierarchyLevel == category.hierarchyLevel && it.active
        } ?: return
        viewModelScope.launch {
            when (val result = repository.mergeCategories(category.id, destination.id)) {
                CategoryMergeResult.Merged -> form.update { it.copy(statusMessage = "Merged into ${destination.displayName}.") }
                is CategoryMergeResult.Rejected -> form.update { it.copy(statusMessage = result.reason) }
            }
        }
    }

    fun generateMorePeriods() {
        viewModelScope.launch {
            val through = state.value.periods.maxOfOrNull { it.endDate }?.plusMonths(3) ?: LocalDate.now().plusMonths(3)
            repository.generatePeriodsForDefaultSpace(LocalDate.now(), through)
            form.update { it.copy(statusMessage = "Future periods generated.") }
        }
    }
}
