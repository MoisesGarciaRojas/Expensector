package com.danielgarcia.expensector.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.danielgarcia.expensector.domain.AccountType
import com.danielgarcia.expensector.domain.BudgetPeriod
import com.danielgarcia.expensector.domain.BudgetPeriodConfiguration
import com.danielgarcia.expensector.domain.BudgetPeriodStatus
import com.danielgarcia.expensector.domain.Category
import com.danielgarcia.expensector.domain.CategoryMergeRecord
import com.danielgarcia.expensector.domain.CreditCardDetails
import com.danielgarcia.expensector.domain.FinancialAccount
import com.danielgarcia.expensector.domain.FinancialPurpose
import com.danielgarcia.expensector.domain.FinancialSpace
import com.danielgarcia.expensector.domain.OpeningBalance
import com.danielgarcia.expensector.domain.OpeningBalanceType
import com.danielgarcia.expensector.domain.PeriodType
import com.danielgarcia.expensector.domain.WeekendAdjustmentStrategy
import java.time.LocalDate

@Entity(tableName = "financial_spaces", indices = [Index("code"), Index("isDefault")])
data class FinancialSpaceEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "budget_period_configurations",
    foreignKeys = [ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("financialSpaceId"), Index("active")],
)
data class BudgetPeriodConfigurationEntity(
    @PrimaryKey val id: String,
    val financialSpaceId: String,
    val periodType: PeriodType,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val weekendAdjustmentStrategy: WeekendAdjustmentStrategy,
    val active: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "budget_periods",
    foreignKeys = [
        ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(BudgetPeriodConfigurationEntity::class, ["id"], ["configurationId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("financialSpaceId"), Index("configurationId"), Index(value = ["configurationId", "periodKey"], unique = true)],
)
data class BudgetPeriodEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "financial_accounts",
    foreignKeys = [ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("financialSpaceId"), Index("accountType")],
)
data class FinancialAccountEntity(
    @PrimaryKey val id: String,
    val financialSpaceId: String,
    val accountType: AccountType,
    val displayName: String,
    val normalizedName: String,
    val institutionName: String?,
    val normalizedInstitutionName: String?,
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

@Entity(
    tableName = "credit_card_details",
    primaryKeys = ["financialAccountId"],
    foreignKeys = [ForeignKey(FinancialAccountEntity::class, ["id"], ["financialAccountId"], onDelete = ForeignKey.CASCADE)],
)
data class CreditCardDetailsEntity(
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
)

@Entity(
    tableName = "opening_balances",
    foreignKeys = [
        ForeignKey(FinancialAccountEntity::class, ["id"], ["financialAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("financialAccountId"), Index("financialSpaceId")],
)
data class OpeningBalanceEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["parentCategoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("financialSpaceId"), Index("parentCategoryId"), Index("purpose"), Index("normalizedName")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
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

@Entity(
    tableName = "category_merge_records",
    foreignKeys = [
        ForeignKey(FinancialSpaceEntity::class, ["id"], ["financialSpaceId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["sourceCategoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(CategoryEntity::class, ["id"], ["destinationCategoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("financialSpaceId"), Index("sourceCategoryId"), Index("destinationCategoryId")],
)
data class CategoryMergeRecordEntity(
    @PrimaryKey val id: String,
    val financialSpaceId: String,
    val sourceCategoryId: String,
    val destinationCategoryId: String,
    val createdAtEpochMillis: Long,
)

fun FinancialSpaceEntity.toDomain() = FinancialSpace(id, code, displayName, description, defaultCurrencyCode, isDefault, active, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun FinancialSpace.toEntity() = FinancialSpaceEntity(id, code, displayName, description, defaultCurrencyCode, isDefault, active, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun BudgetPeriodConfigurationEntity.toDomain() = BudgetPeriodConfiguration(id, financialSpaceId, periodType, effectiveFrom, effectiveTo, weekendAdjustmentStrategy, active, createdAtEpochMillis, updatedAtEpochMillis)
fun BudgetPeriodConfiguration.toEntity() = BudgetPeriodConfigurationEntity(id, financialSpaceId, periodType, effectiveFrom, effectiveTo, weekendAdjustmentStrategy, active, createdAtEpochMillis, updatedAtEpochMillis)
fun BudgetPeriodEntity.toDomain() = BudgetPeriod(id, financialSpaceId, configurationId, periodKey, periodType, startDate, endDate, nominalExpectedPaymentDate, adjustedExpectedPaymentDate, actualPaymentDate, status, createdAtEpochMillis, updatedAtEpochMillis)
fun BudgetPeriod.toEntity() = BudgetPeriodEntity(id, financialSpaceId, configurationId, periodKey, periodType, startDate, endDate, nominalExpectedPaymentDate, adjustedExpectedPaymentDate, actualPaymentDate, status, createdAtEpochMillis, updatedAtEpochMillis)
fun FinancialAccountEntity.toDomain() = FinancialAccount(id, financialSpaceId, accountType, displayName, institutionName, alias, currencyCode, lastFourDigits, displayOrder, active, includeInAvailableCash, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun FinancialAccount.toEntity() = FinancialAccountEntity(id, financialSpaceId, accountType, displayName, com.danielgarcia.expensector.domain.normalizeCatalogName(displayName), institutionName, institutionName?.let { com.danielgarcia.expensector.domain.normalizeCatalogName(it) }, alias, currencyCode, lastFourDigits, displayOrder, active, includeInAvailableCash, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun CreditCardDetailsEntity.toDomain() = CreditCardDetails(financialAccountId, issuer, productName, creditLimitMinor, currencyCode, statementClosingDay, paymentDueDay, annualInterestRateBasisPoints, active, createdAtEpochMillis, updatedAtEpochMillis)
fun CreditCardDetails.toEntity() = CreditCardDetailsEntity(financialAccountId, issuer, productName, creditLimitMinor, currencyCode, statementClosingDay, paymentDueDay, annualInterestRateBasisPoints, active, createdAtEpochMillis, updatedAtEpochMillis)
fun OpeningBalanceEntity.toDomain() = OpeningBalance(id, financialAccountId, financialSpaceId, amountMinor, currencyCode, effectiveDate, type, note, createdAtEpochMillis, updatedAtEpochMillis)
fun OpeningBalance.toEntity() = OpeningBalanceEntity(id, financialAccountId, financialSpaceId, amountMinor, currencyCode, effectiveDate, type, note, createdAtEpochMillis, updatedAtEpochMillis)
fun CategoryEntity.toDomain() = Category(id, financialSpaceId, purpose, displayName, normalizedName, description, parentCategoryId, hierarchyLevel, displayOrder, active, systemProvided, mergedIntoCategoryId, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun Category.toEntity() = CategoryEntity(id, financialSpaceId, purpose, displayName, normalizedName, description, parentCategoryId, hierarchyLevel, displayOrder, active, systemProvided, mergedIntoCategoryId, createdAtEpochMillis, updatedAtEpochMillis, archivedAtEpochMillis)
fun CategoryMergeRecord.toEntity() = CategoryMergeRecordEntity(id, financialSpaceId, sourceCategoryId, destinationCategoryId, createdAtEpochMillis)
