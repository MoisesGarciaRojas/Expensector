package com.danielgarcia.expensector.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        LocalOwnerProfileEntity::class,
        FinancialSpaceEntity::class,
        BudgetPeriodConfigurationEntity::class,
        BudgetPeriodEntity::class,
        FinancialAccountEntity::class,
        CreditCardDetailsEntity::class,
        OpeningBalanceEntity::class,
        CategoryEntity::class,
        CategoryMergeRecordEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class ExpensectorDatabase : RoomDatabase() {
    abstract fun localOwnerProfileDao(): LocalOwnerProfileDao
    abstract fun stage2Dao(): Stage2Dao
}
