package com.danielgarcia.expensector.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.danielgarcia.expensector.domain.AccountType
import com.danielgarcia.expensector.domain.FinancialPurpose
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface Stage2Dao {
    @Query("SELECT * FROM financial_spaces ORDER BY isDefault DESC, displayName")
    fun observeFinancialSpaces(): Flow<List<FinancialSpaceEntity>>

    @Query("SELECT * FROM budget_periods ORDER BY startDate")
    fun observeBudgetPeriods(): Flow<List<BudgetPeriodEntity>>

    @Query("SELECT * FROM financial_accounts ORDER BY active DESC, accountType, displayOrder, displayName")
    fun observeAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM credit_card_details")
    fun observeCreditCardDetails(): Flow<List<CreditCardDetailsEntity>>

    @Query("SELECT * FROM opening_balances ORDER BY effectiveDate, createdAtEpochMillis")
    fun observeOpeningBalances(): Flow<List<OpeningBalanceEntity>>

    @Query("SELECT * FROM categories ORDER BY purpose, hierarchyLevel, displayOrder, displayName")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM financial_spaces WHERE code = :code LIMIT 1")
    suspend fun getSpaceByCode(code: String): FinancialSpaceEntity?

    @Query("SELECT * FROM financial_spaces WHERE id = :id LIMIT 1")
    suspend fun getSpace(id: String): FinancialSpaceEntity?

    @Query("SELECT * FROM financial_spaces WHERE isDefault = 1 AND active = 1 LIMIT 1")
    suspend fun getDefaultSpace(): FinancialSpaceEntity?

    @Query("UPDATE financial_spaces SET isDefault = 0, updatedAtEpochMillis = :now")
    suspend fun clearDefaultSpaces(now: Long)

    @Upsert
    suspend fun upsertSpace(space: FinancialSpaceEntity)

    @Query("UPDATE financial_spaces SET active = :active, archivedAtEpochMillis = :archivedAt, updatedAtEpochMillis = :now WHERE id = :id AND code IS NOT 'PERSONAL'")
    suspend fun setSpaceArchiveState(id: String, active: Boolean, archivedAt: Long?, now: Long)

    @Query("SELECT * FROM budget_period_configurations WHERE financialSpaceId = :spaceId AND active = 1 LIMIT 1")
    suspend fun getActiveConfiguration(spaceId: String): BudgetPeriodConfigurationEntity?

    @Upsert
    suspend fun upsertConfiguration(configuration: BudgetPeriodConfigurationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPeriods(periods: List<BudgetPeriodEntity>)

    @Query("UPDATE budget_periods SET actualPaymentDate = :date, updatedAtEpochMillis = :now WHERE id = :periodId")
    suspend fun updateActualPaymentDate(periodId: String, date: LocalDate?, now: Long)

    @Upsert
    suspend fun upsertAccount(account: FinancialAccountEntity)

    @Upsert
    suspend fun upsertCreditCardDetails(details: CreditCardDetailsEntity)

    @Query("DELETE FROM credit_card_details WHERE financialAccountId = :accountId")
    suspend fun deleteCreditCardDetails(accountId: String)

    @Upsert
    suspend fun upsertOpeningBalance(balance: OpeningBalanceEntity)

    @Query("UPDATE financial_accounts SET active = :active, archivedAtEpochMillis = :archivedAt, updatedAtEpochMillis = :now WHERE id = :accountId")
    suspend fun setAccountArchiveState(accountId: String, active: Boolean, archivedAt: Long?, now: Long)

    @Query("SELECT * FROM financial_accounts WHERE financialSpaceId = :spaceId AND accountType = :type AND normalizedName = :normalizedName AND (:normalizedInstitution IS NULL OR normalizedInstitutionName = :normalizedInstitution) AND (:lastFour IS NULL OR lastFourDigits = :lastFour) AND archivedAtEpochMillis IS NULL LIMIT 1")
    suspend fun findDuplicateAccount(spaceId: String, type: AccountType, normalizedName: String, normalizedInstitution: String?, lastFour: String?): FinancialAccountEntity?

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategory(id: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId")
    suspend fun getCategoryChildren(parentId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE financialSpaceId = :spaceId AND purpose = :purpose AND normalizedName = :normalizedName AND ((parentCategoryId IS NULL AND :parentId IS NULL) OR parentCategoryId = :parentId) AND archivedAtEpochMillis IS NULL AND mergedIntoCategoryId IS NULL LIMIT 1")
    suspend fun findDuplicateCategory(spaceId: String, purpose: FinancialPurpose, parentId: String?, normalizedName: String): CategoryEntity?

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Insert
    suspend fun insertMergeRecord(record: CategoryMergeRecordEntity)

    @Query("UPDATE categories SET active = :active, archivedAtEpochMillis = :archivedAt, updatedAtEpochMillis = :now WHERE id = :categoryId")
    suspend fun setCategoryArchiveState(categoryId: String, active: Boolean, archivedAt: Long?, now: Long)

    @Transaction
    suspend fun saveAccountWithDetails(account: FinancialAccountEntity, openingBalance: OpeningBalanceEntity?, creditCardDetails: CreditCardDetailsEntity?) {
        upsertAccount(account)
        if (account.accountType == AccountType.CREDIT_CARD && creditCardDetails != null) {
            upsertCreditCardDetails(creditCardDetails)
        } else {
            deleteCreditCardDetails(account.id)
        }
        if (openingBalance != null) upsertOpeningBalance(openingBalance)
    }

    @Transaction
    suspend fun mergeCategory(source: CategoryEntity, movedChildren: List<CategoryEntity>, record: CategoryMergeRecordEntity) {
        if (movedChildren.isNotEmpty()) updateCategories(movedChildren)
        upsertCategory(source)
        insertMergeRecord(record)
    }
}
