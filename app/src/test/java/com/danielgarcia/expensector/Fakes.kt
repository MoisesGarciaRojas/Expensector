package com.danielgarcia.expensector

import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.domain.BudgetPeriod
import com.danielgarcia.expensector.domain.Category
import com.danielgarcia.expensector.domain.CategoryMergeResult
import com.danielgarcia.expensector.domain.CategorySaveResult
import com.danielgarcia.expensector.domain.CreditCardDetails
import com.danielgarcia.expensector.domain.FinancialAccount
import com.danielgarcia.expensector.domain.FinancialSpace
import com.danielgarcia.expensector.domain.OpeningBalance
import com.danielgarcia.expensector.domain.Stage2HomeSummary
import com.danielgarcia.expensector.domain.Stage2Repository
import com.danielgarcia.expensector.platform.Clock
import com.danielgarcia.expensector.security.PinSecurityRepository
import com.danielgarcia.expensector.security.PinVerificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

class FakeClock(var now: Long = 0L) : Clock {
    override fun nowEpochMillis(): Long = now
}

class FakeProfileRepository : LocalOwnerProfileRepository {
    val profile = MutableStateFlow<LocalOwnerProfile?>(null)
    override fun observeProfile(): Flow<LocalOwnerProfile?> = profile
    override suspend fun saveProfile(profile: LocalOwnerProfile) {
        this.profile.value = profile
    }
}

class FakePreferencesRepository : AppPreferencesRepository {
    val preferences = MutableStateFlow(
        AppPreferences(
            onboardingCompleted = false,
            biometricEnabled = false,
            lockDuration = LockDuration.Default,
            lastBackgroundAtEpochMillis = null,
        ),
    )

    override fun observePreferences(): Flow<AppPreferences> = preferences
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.value = preferences.value.copy(onboardingCompleted = completed)
    }
    override suspend fun setBiometricEnabled(enabled: Boolean) {
        preferences.value = preferences.value.copy(biometricEnabled = enabled)
    }
    override suspend fun setLockDuration(duration: LockDuration) {
        preferences.value = preferences.value.copy(lockDuration = duration)
    }
    override suspend fun setLastBackgroundAt(timestampMillis: Long?) {
        preferences.value = preferences.value.copy(lastBackgroundAtEpochMillis = timestampMillis)
    }
}

class FakeStage2Repository : Stage2Repository {
    var initialized = false
    override fun observeHomeSummary(today: LocalDate): Flow<Stage2HomeSummary> = MutableStateFlow(Stage2HomeSummary(null, null, null, null, 0, 0, 0, 0))
    override fun observeFinancialSpaces(): Flow<List<FinancialSpace>> = MutableStateFlow(emptyList())
    override fun observeBudgetPeriods(): Flow<List<BudgetPeriod>> = MutableStateFlow(emptyList())
    override fun observeAccounts(): Flow<List<FinancialAccount>> = MutableStateFlow(emptyList())
    override fun observeCreditCardDetails(): Flow<List<CreditCardDetails>> = MutableStateFlow(emptyList())
    override fun observeOpeningBalances(): Flow<List<OpeningBalance>> = MutableStateFlow(emptyList())
    override fun observeCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
    override suspend fun initializeDefaults(displayName: String, currencyCode: String, trackingStartDate: LocalDate) {
        initialized = true
    }
    override suspend fun saveFinancialSpace(space: FinancialSpace) = Unit
    override suspend fun setDefaultFinancialSpace(spaceId: String) = Unit
    override suspend fun setFinancialSpaceArchived(spaceId: String, archived: Boolean) = Unit
    override suspend fun generatePeriodsForDefaultSpace(from: LocalDate, through: LocalDate) = Unit
    override suspend fun updateActualPaymentDate(periodId: String, actualPaymentDate: LocalDate?) = Unit
    override suspend fun saveAccount(account: FinancialAccount, openingBalance: OpeningBalance?, creditCardDetails: CreditCardDetails?) = Unit
    override suspend fun setAccountArchived(accountId: String, archived: Boolean) = Unit
    override suspend fun saveCategory(category: Category): CategorySaveResult = CategorySaveResult.Saved(category)
    override suspend fun setCategoryArchived(categoryId: String, archived: Boolean) = Unit
    override suspend fun mergeCategories(sourceCategoryId: String, destinationCategoryId: String): CategoryMergeResult = CategoryMergeResult.Merged
}

class FakePinSecurityRepository : PinSecurityRepository {
    var pin: String? = null
    var verifyResult: PinVerificationResult? = null

    override suspend fun hasPin(): Boolean = pin != null
    override suspend fun setPin(pin: String) {
        this.pin = pin
    }
    override suspend fun verifyPin(pin: String): PinVerificationResult =
        verifyResult ?: if (pin == this.pin) PinVerificationResult.Success else PinVerificationResult.InvalidPin

    override suspend fun changePin(currentPin: String, newPin: String): PinVerificationResult {
        val result = verifyPin(currentPin)
        if (result == PinVerificationResult.Success) pin = newPin
        return result
    }
}
