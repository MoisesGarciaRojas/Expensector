package com.danielgarcia.expensector.domain

import kotlinx.coroutines.flow.Flow

interface LocalOwnerProfileRepository {
    fun observeProfile(): Flow<LocalOwnerProfile?>
    suspend fun saveProfile(profile: LocalOwnerProfile)
}

interface AppPreferencesRepository {
    fun observePreferences(): Flow<AppPreferences>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setLockDuration(duration: LockDuration)
    suspend fun setLastBackgroundAt(timestampMillis: Long?)
}

interface Stage2Repository {
    fun observeHomeSummary(today: java.time.LocalDate): Flow<Stage2HomeSummary>
    fun observeFinancialSpaces(): Flow<List<FinancialSpace>>
    fun observeBudgetPeriods(): Flow<List<BudgetPeriod>>
    fun observeAccounts(): Flow<List<FinancialAccount>>
    fun observeCreditCardDetails(): Flow<List<CreditCardDetails>>
    fun observeOpeningBalances(): Flow<List<OpeningBalance>>
    fun observeCategories(): Flow<List<Category>>

    suspend fun initializeDefaults(displayName: String = "Personal", currencyCode: String = "MXN", trackingStartDate: java.time.LocalDate = java.time.LocalDate.now())
    suspend fun saveFinancialSpace(space: FinancialSpace)
    suspend fun setDefaultFinancialSpace(spaceId: String)
    suspend fun setFinancialSpaceArchived(spaceId: String, archived: Boolean)
    suspend fun generatePeriodsForDefaultSpace(from: java.time.LocalDate, through: java.time.LocalDate)
    suspend fun updateActualPaymentDate(periodId: String, actualPaymentDate: java.time.LocalDate?)
    suspend fun saveAccount(account: FinancialAccount, openingBalance: OpeningBalance?, creditCardDetails: CreditCardDetails?)
    suspend fun setAccountArchived(accountId: String, archived: Boolean)
    suspend fun saveCategory(category: Category): CategorySaveResult
    suspend fun setCategoryArchived(categoryId: String, archived: Boolean)
    suspend fun mergeCategories(sourceCategoryId: String, destinationCategoryId: String): CategoryMergeResult
}

sealed interface CategorySaveResult {
    data class Saved(val category: Category) : CategorySaveResult
    data class Duplicate(val existing: Category) : CategorySaveResult
}

sealed interface CategoryMergeResult {
    data object Merged : CategoryMergeResult
    data class Rejected(val reason: String) : CategoryMergeResult
}
