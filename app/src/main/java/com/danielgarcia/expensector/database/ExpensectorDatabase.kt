package com.danielgarcia.expensector.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LocalOwnerProfileEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ExpensectorDatabase : RoomDatabase() {
    abstract fun localOwnerProfileDao(): LocalOwnerProfileDao
}
