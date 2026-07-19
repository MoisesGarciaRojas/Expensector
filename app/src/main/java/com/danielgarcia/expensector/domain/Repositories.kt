package com.danielgarcia.expensector.domain

import kotlinx.coroutines.flow.Flow

interface LocalOwnerProfileRepository {
    fun observeProfile(): Flow<LocalOwnerProfile?>
    suspend fun saveProfile(profile: LocalOwnerProfile)
}

interface AppPreferencesRepository {
    fun observePreferences(): Flow<AppPreferences>
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setLockDuration(duration: LockDuration)
    suspend fun setLastBackgroundAt(timestampMillis: Long?)
}
