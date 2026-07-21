package com.danielgarcia.expensector.domain

import java.text.Normalizer
import java.time.LocalDate

enum class PeriodType { WEEKLY, BIWEEKLY, SEMIMONTHLY, MONTHLY, CUSTOM }
enum class WeekendAdjustmentStrategy { PREVIOUS_FRIDAY }
enum class BudgetPeriodStatus { UPCOMING, CURRENT, CLOSED }
enum class AccountType { CASH, CHECKING, SAVINGS, CREDIT_CARD, DIGITAL_WALLET, INVESTMENT }
enum class FinancialPurpose { NEED, WANT, INVESTMENT }
enum class OpeningBalanceType { ASSET_BALANCE, CREDIT_CARD_OUTSTANDING }

data class FinancialSpace(
    val id: String,
    val code: String?,
    val displayName: String,
    val description: String?,
    val defaultCurrencyCode: String,
    val isDefault: Boolean,
    val active: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

data class BudgetPeriodConfiguration(
    val id: String,
    val financialSpaceId: String,
    val periodType: PeriodType,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val weekendAdjustmentStrategy: WeekendAdjustmentStrategy,
    val active: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class BudgetPeriod(
    val id: String,
    val financialSpaceId: String,
    val configurationId: String,
    val periodKey: String,
    val periodType: PeriodType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val nominalExpectedPaymentDate: LocalDate,
    val adjustedExpectedPaymentDate: LocalDate,
    val actualPaymentDate: LocalDate?,
    val status: BudgetPeriodStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class FinancialAccount(
    val id: String,
    val financialSpaceId: String,
    val accountType: AccountType,
    val displayName: String,
    val institutionName: String?,
    val alias: String?,
    val currencyCode: String,
    val lastFourDigits: String?,
    val displayOrder: Int,
    val active: Boolean,
    val includeInAvailableCash: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

data class CreditCardDetails(
    val financialAccountId: String,
    val issuer: String,
    val productName: String?,
    val creditLimitMinor: Long?,
    val currencyCode: String,
    val statementClosingDay: Int,
    val paymentDueDay: Int,
    val annualInterestRateBasisPoints: Int?,
    val active: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun initialAvailableCredit(openingOutstandingMinor: Long): Long? =
        creditLimitMinor?.minus(openingOutstandingMinor)
}

data class OpeningBalance(
    val id: String,
    val financialAccountId: String,
    val financialSpaceId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val effectiveDate: LocalDate,
    val type: OpeningBalanceType,
    val note: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class Category(
    val id: String,
    val financialSpaceId: String,
    val purpose: FinancialPurpose,
    val displayName: String,
    val normalizedName: String,
    val description: String?,
    val parentCategoryId: String?,
    val hierarchyLevel: Int,
    val displayOrder: Int,
    val active: Boolean,
    val systemProvided: Boolean,
    val mergedIntoCategoryId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val archivedAtEpochMillis: Long?,
)

data class CategoryMergeRecord(
    val id: String,
    val financialSpaceId: String,
    val sourceCategoryId: String,
    val destinationCategoryId: String,
    val createdAtEpochMillis: Long,
)

data class Stage2HomeSummary(
    val defaultSpace: FinancialSpace?,
    val currentPeriod: BudgetPeriod?,
    val previousPeriod: BudgetPeriod?,
    val nextPeriod: BudgetPeriod?,
    val activeAccountCount: Int,
    val openingAssetMinor: Long,
    val openingCreditCardOutstandingMinor: Long,
    val categoryCount: Int,
)

fun normalizeCatalogName(value: String): String {
    val compact = value.trim().replace(Regex("\\s+"), " ").lowercase()
    val decomposed = Normalizer.normalize(compact, Normalizer.Form.NFD)
    return decomposed.replace(Regex("\\p{Mn}+"), "")
}

fun defaultIncludeInAvailableCash(type: AccountType): Boolean =
    when (type) {
        AccountType.CASH, AccountType.CHECKING -> true
        AccountType.SAVINGS, AccountType.DIGITAL_WALLET -> true
        AccountType.CREDIT_CARD, AccountType.INVESTMENT -> false
    }
