package com.danielgarcia.expensector.domain

data class LocalOwnerProfile(
    val id: String,
    val displayName: String,
    val email: String?,
    val defaultCurrencyCode: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
