package com.danielgarcia.expensector.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalOwnerProfileDao {
    @Query("SELECT * FROM local_owner_profiles LIMIT 1")
    fun observeProfile(): Flow<LocalOwnerProfileEntity?>

    @Upsert
    suspend fun upsert(profile: LocalOwnerProfileEntity)
}
