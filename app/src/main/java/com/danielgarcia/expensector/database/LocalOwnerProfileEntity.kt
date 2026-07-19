package com.danielgarcia.expensector.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.danielgarcia.expensector.domain.LocalOwnerProfile

@Entity(tableName = "local_owner_profiles")
data class LocalOwnerProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val email: String?,
    val defaultCurrencyCode: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

fun LocalOwnerProfileEntity.toDomain(): LocalOwnerProfile =
    LocalOwnerProfile(
        id = id,
        displayName = displayName,
        email = email,
        defaultCurrencyCode = defaultCurrencyCode,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

fun LocalOwnerProfile.toEntity(): LocalOwnerProfileEntity =
    LocalOwnerProfileEntity(
        id = id,
        displayName = displayName,
        email = email,
        defaultCurrencyCode = defaultCurrencyCode,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )
