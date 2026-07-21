package com.danielgarcia.expensector.data

import com.danielgarcia.expensector.database.Stage2Dao
import com.danielgarcia.expensector.database.toDomain
import com.danielgarcia.expensector.database.toEntity
import com.danielgarcia.expensector.domain.BudgetPeriodConfiguration
import com.danielgarcia.expensector.domain.BudgetPeriodGenerator
import com.danielgarcia.expensector.domain.Category
import com.danielgarcia.expensector.domain.CategoryMergeRecord
import com.danielgarcia.expensector.domain.CategoryMergeResult
import com.danielgarcia.expensector.domain.CategorySaveResult
import com.danielgarcia.expensector.domain.CreditCardDetails
import com.danielgarcia.expensector.domain.FinancialAccount
import com.danielgarcia.expensector.domain.FinancialPurpose
import com.danielgarcia.expensector.domain.FinancialSpace
import com.danielgarcia.expensector.domain.OpeningBalance
import com.danielgarcia.expensector.domain.PeriodType
import com.danielgarcia.expensector.domain.Stage2HomeSummary
import com.danielgarcia.expensector.domain.Stage2Repository
import com.danielgarcia.expensector.domain.WeekendAdjustmentStrategy
import com.danielgarcia.expensector.domain.normalizeCatalogName
import com.danielgarcia.expensector.domain.openingPeriodWindow
import com.danielgarcia.expensector.platform.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RoomStage2Repository @Inject constructor(
    private val dao: Stage2Dao,
    private val clock: Clock,
) : Stage2Repository {
    private val generator = BudgetPeriodGenerator()

    override fun observeFinancialSpaces(): Flow<List<FinancialSpace>> =
        dao.observeFinancialSpaces().map { rows -> rows.map { it.toDomain() } }

    override fun observeBudgetPeriods() =
        dao.observeBudgetPeriods().map { rows -> rows.map { it.toDomain() } }

    override fun observeAccounts() =
        dao.observeAccounts().map { rows -> rows.map { it.toDomain() } }

    override fun observeCreditCardDetails() =
        dao.observeCreditCardDetails().map { rows -> rows.map { it.toDomain() } }

    override fun observeOpeningBalances() =
        dao.observeOpeningBalances().map { rows -> rows.map { it.toDomain() } }

    override fun observeCategories() =
        dao.observeCategories().map { rows -> rows.map { it.toDomain() } }

    override fun observeHomeSummary(today: LocalDate): Flow<Stage2HomeSummary> =
        combine(
            observeFinancialSpaces(),
            observeBudgetPeriods(),
            observeAccounts(),
            observeOpeningBalances(),
            observeCategories(),
        ) { spaces, periods, accounts, balances, categories ->
            val defaultSpace = spaces.firstOrNull { it.isDefault && it.active }
            val spacePeriods = periods.filter { it.financialSpaceId == defaultSpace?.id }
            Stage2HomeSummary(
                defaultSpace = defaultSpace,
                currentPeriod = spacePeriods.firstOrNull { !today.isBefore(it.startDate) && !today.isAfter(it.endDate) },
                previousPeriod = spacePeriods.filter { it.endDate.isBefore(today) }.maxByOrNull { it.endDate },
                nextPeriod = spacePeriods.filter { it.startDate.isAfter(today) }.minByOrNull { it.startDate },
                activeAccountCount = accounts.count { it.financialSpaceId == defaultSpace?.id && it.active },
                openingAssetMinor = balances
                    .filter { it.financialSpaceId == defaultSpace?.id && it.type.name == "ASSET_BALANCE" }
                    .sumOf { it.amountMinor },
                openingCreditCardOutstandingMinor = balances
                    .filter { it.financialSpaceId == defaultSpace?.id && it.type.name == "CREDIT_CARD_OUTSTANDING" }
                    .sumOf { it.amountMinor },
                categoryCount = categories.count { it.financialSpaceId == defaultSpace?.id && it.active },
            )
        }

    override suspend fun initializeDefaults(displayName: String, currencyCode: String, trackingStartDate: LocalDate) {
        val now = clock.nowEpochMillis()
        val existing = dao.getSpaceByCode(PERSONAL_CODE)
        val space = existing?.toDomain() ?: FinancialSpace(
            id = UUID.randomUUID().toString(),
            code = PERSONAL_CODE,
            displayName = displayName,
            description = null,
            defaultCurrencyCode = currencyCode,
            isDefault = true,
            active = true,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
            archivedAtEpochMillis = null,
        )
        if (existing == null) {
            dao.clearDefaultSpaces(now)
            dao.upsertSpace(space.toEntity())
        }
        val configuration = dao.getActiveConfiguration(space.id)?.toDomain() ?: BudgetPeriodConfiguration(
            id = UUID.randomUUID().toString(),
            financialSpaceId = space.id,
            periodType = PeriodType.SEMIMONTHLY,
            effectiveFrom = trackingStartDate,
            effectiveTo = null,
            weekendAdjustmentStrategy = WeekendAdjustmentStrategy.PREVIOUS_FRIDAY,
            active = true,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        dao.upsertConfiguration(configuration.toEntity())
        val (from, through) = openingPeriodWindow(trackingStartDate)
        dao.insertPeriods(generator.generateSemimonthly(configuration, from, through, now, trackingStartDate).map { it.toEntity() })
        insertStarterCategories(space.id, now)
    }

    override suspend fun saveFinancialSpace(space: FinancialSpace) {
        if (space.isDefault) dao.clearDefaultSpaces(clock.nowEpochMillis())
        dao.upsertSpace(space.toEntity())
    }

    override suspend fun setDefaultFinancialSpace(spaceId: String) {
        val now = clock.nowEpochMillis()
        val space = dao.getSpace(spaceId)?.toDomain() ?: return
        dao.clearDefaultSpaces(now)
        dao.upsertSpace(space.copy(isDefault = true, active = true, archivedAtEpochMillis = null, updatedAtEpochMillis = now).toEntity())
    }

    override suspend fun setFinancialSpaceArchived(spaceId: String, archived: Boolean) {
        val now = clock.nowEpochMillis()
        dao.setSpaceArchiveState(spaceId, active = !archived, archivedAt = if (archived) now else null, now = now)
    }

    override suspend fun generatePeriodsForDefaultSpace(from: LocalDate, through: LocalDate) {
        val space = dao.getDefaultSpace() ?: return
        val configuration = dao.getActiveConfiguration(space.id)?.toDomain() ?: return
        dao.insertPeriods(generator.generateSemimonthly(configuration, from, through, clock.nowEpochMillis()).map { it.toEntity() })
    }

    override suspend fun updateActualPaymentDate(periodId: String, actualPaymentDate: LocalDate?) {
        dao.updateActualPaymentDate(periodId, actualPaymentDate, clock.nowEpochMillis())
    }

    override suspend fun saveAccount(account: FinancialAccount, openingBalance: OpeningBalance?, creditCardDetails: CreditCardDetails?) {
        require(account.displayName.isNotBlank()) { "Account name is required." }
        require(account.lastFourDigits == null || account.lastFourDigits.matches(Regex("\\d{4}"))) { "Last four digits must be four numeric digits." }
        val duplicate = dao.findDuplicateAccount(
            account.financialSpaceId,
            account.accountType,
            normalizeCatalogName(account.displayName),
            account.institutionName?.let(::normalizeCatalogName),
            account.lastFourDigits,
        )
        require(duplicate == null || duplicate.id == account.id) { "A similar account already exists." }
        dao.saveAccountWithDetails(account.toEntity(), openingBalance?.toEntity(), creditCardDetails?.toEntity())
    }

    override suspend fun setAccountArchived(accountId: String, archived: Boolean) {
        val now = clock.nowEpochMillis()
        dao.setAccountArchiveState(accountId, !archived, if (archived) now else null, now)
    }

    override suspend fun saveCategory(category: Category): CategorySaveResult {
        val normalized = normalizeCatalogName(category.displayName)
        val candidate = category.copy(normalizedName = normalized)
        val duplicate = dao.findDuplicateCategory(candidate.financialSpaceId, candidate.purpose, candidate.parentCategoryId, normalized)?.toDomain()
        return if (duplicate != null && duplicate.id != candidate.id) {
            CategorySaveResult.Duplicate(duplicate)
        } else {
            dao.upsertCategory(candidate.toEntity())
            CategorySaveResult.Saved(candidate)
        }
    }

    override suspend fun setCategoryArchived(categoryId: String, archived: Boolean) {
        val now = clock.nowEpochMillis()
        dao.setCategoryArchiveState(categoryId, !archived, if (archived) now else null, now)
    }

    override suspend fun mergeCategories(sourceCategoryId: String, destinationCategoryId: String): CategoryMergeResult {
        if (sourceCategoryId == destinationCategoryId) return CategoryMergeResult.Rejected("Choose two different categories.")
        val now = clock.nowEpochMillis()
        val source = dao.getCategory(sourceCategoryId)?.toDomain() ?: return CategoryMergeResult.Rejected("Source category was not found.")
        val destination = dao.getCategory(destinationCategoryId)?.toDomain() ?: return CategoryMergeResult.Rejected("Destination category was not found.")
        if (source.financialSpaceId != destination.financialSpaceId) return CategoryMergeResult.Rejected("Categories must be in the same financial space.")
        if (source.purpose != destination.purpose) return CategoryMergeResult.Rejected("Cross-purpose category merges are not allowed in Stage 2.")
        if (source.hierarchyLevel != destination.hierarchyLevel) return CategoryMergeResult.Rejected("Categories must have the same hierarchy level.")
        val sourceChildren = dao.getCategoryChildren(source.id).map { it.toDomain() }
        val destinationChildren = dao.getCategoryChildren(destination.id).map { it.toDomain() }
        val duplicateChild = sourceChildren.firstOrNull { child ->
            destinationChildren.any { it.normalizedName == child.normalizedName && it.purpose == child.purpose }
        }
        if (duplicateChild != null) return CategoryMergeResult.Rejected("Resolve duplicate child category '${duplicateChild.displayName}' before merging.")
        val movedChildren = sourceChildren.map { it.copy(parentCategoryId = destination.id, updatedAtEpochMillis = now).toEntity() }
        dao.mergeCategory(
            source.copy(active = false, mergedIntoCategoryId = destination.id, archivedAtEpochMillis = now, updatedAtEpochMillis = now).toEntity(),
            movedChildren,
            CategoryMergeRecord(UUID.randomUUID().toString(), source.financialSpaceId, source.id, destination.id, now).toEntity(),
        )
        return CategoryMergeResult.Merged
    }

    private suspend fun insertStarterCategories(spaceId: String, now: Long) {
        starterCategories().forEachIndexed { index, seed ->
            val root = Category(
                id = UUID.nameUUIDFromBytes("$spaceId:${seed.purpose}:${seed.name}".toByteArray()).toString(),
                financialSpaceId = spaceId,
                purpose = seed.purpose,
                displayName = seed.name,
                normalizedName = normalizeCatalogName(seed.name),
                description = null,
                parentCategoryId = null,
                hierarchyLevel = 0,
                displayOrder = index,
                active = true,
                systemProvided = true,
                mergedIntoCategoryId = null,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
                archivedAtEpochMillis = null,
            )
            if (dao.findDuplicateCategory(spaceId, root.purpose, null, root.normalizedName) == null) dao.upsertCategory(root.toEntity())
            seed.children.forEachIndexed { childIndex, childName ->
                val child = root.copy(
                    id = UUID.nameUUIDFromBytes("$spaceId:${seed.purpose}:${seed.name}:$childName".toByteArray()).toString(),
                    displayName = childName,
                    normalizedName = normalizeCatalogName(childName),
                    parentCategoryId = root.id,
                    hierarchyLevel = 1,
                    displayOrder = childIndex,
                )
                if (dao.findDuplicateCategory(spaceId, child.purpose, root.id, child.normalizedName) == null) dao.upsertCategory(child.toEntity())
            }
        }
    }

    private data class CategorySeed(val purpose: FinancialPurpose, val name: String, val children: List<String> = emptyList())

    private fun starterCategories() = listOf(
        CategorySeed(FinancialPurpose.NEED, "Housing", listOf("Rent or mortgage", "Maintenance", "Property-related payments")),
        CategorySeed(FinancialPurpose.NEED, "Food", listOf("Groceries", "Essential meals")),
        CategorySeed(FinancialPurpose.NEED, "Health", listOf("Medical consultations", "Medication", "Laboratory tests", "Medical imaging")),
        CategorySeed(FinancialPurpose.NEED, "Transportation", listOf("Fuel", "Vehicle maintenance", "Public transportation", "Parking", "Vehicle payment")),
        CategorySeed(FinancialPurpose.NEED, "Utilities and services", listOf("Mobile telephone", "Home telephone", "Internet", "Electricity", "Water", "Gas")),
        CategorySeed(FinancialPurpose.NEED, "Insurance"),
        CategorySeed(FinancialPurpose.NEED, "Debt obligations"),
        CategorySeed(FinancialPurpose.NEED, "Essential personal care"),
        CategorySeed(FinancialPurpose.WANT, "Entertainment", listOf("Movies and events", "Games", "Digital entertainment")),
        CategorySeed(FinancialPurpose.WANT, "Restaurants and outings", listOf("Restaurants", "Cafes", "Bars", "Social outings")),
        CategorySeed(FinancialPurpose.WANT, "Travel"),
        CategorySeed(FinancialPurpose.WANT, "Hobbies"),
        CategorySeed(FinancialPurpose.WANT, "Nonessential shopping"),
        CategorySeed(FinancialPurpose.WANT, "Streaming services", listOf("Video streaming", "Music streaming", "Other subscriptions")),
        CategorySeed(FinancialPurpose.INVESTMENT, "Retirement", listOf("PPR")),
        CategorySeed(FinancialPurpose.INVESTMENT, "Financial investments", listOf("Fixed-income contribution", "Brokerage contribution", "Savings investment")),
        CategorySeed(FinancialPurpose.INVESTMENT, "Business or project investment", listOf("Nutrition application", "Infrastructure", "Software and services", "Marketing")),
        CategorySeed(FinancialPurpose.INVESTMENT, "Education and professional development"),
    )

    companion object {
        const val PERSONAL_CODE = "PERSONAL"
    }
}
