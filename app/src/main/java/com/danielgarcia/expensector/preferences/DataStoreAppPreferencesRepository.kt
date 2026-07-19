package com.danielgarcia.expensector.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LockDuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.expensectorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "expensector_preferences",
)

class DataStoreAppPreferencesRepository @Inject constructor(
    @ApplicationContext
    context: Context,
) : AppPreferencesRepository {
    private val dataStore = context.expensectorDataStore

    override fun observePreferences(): Flow<AppPreferences> =
        dataStore.data.map { preferences ->
            AppPreferences(
                onboardingCompleted = preferences[Keys.OnboardingCompleted] ?: false,
                biometricEnabled = preferences[Keys.BiometricEnabled] ?: false,
                lockDuration = LockDuration.fromPreferenceValue(preferences[Keys.LockDuration]),
                lastBackgroundAtEpochMillis = preferences[Keys.LastBackgroundAt],
            )
        }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.OnboardingCompleted] = completed }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BiometricEnabled] = enabled }
    }

    override suspend fun setLockDuration(duration: LockDuration) {
        dataStore.edit { it[Keys.LockDuration] = duration.preferenceValue }
    }

    override suspend fun setLastBackgroundAt(timestampMillis: Long?) {
        dataStore.edit {
            if (timestampMillis == null) {
                it.remove(Keys.LastBackgroundAt)
            } else {
                it[Keys.LastBackgroundAt] = timestampMillis
            }
        }
    }

    private object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val BiometricEnabled = booleanPreferencesKey("biometric_enabled")
        val LockDuration = stringPreferencesKey("automatic_lock_duration")
        val LastBackgroundAt = longPreferencesKey("last_background_at_epoch_millis")
    }
}
