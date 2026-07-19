package com.danielgarcia.expensector.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.danielgarcia.expensector.data.RoomLocalOwnerProfileRepository
import com.danielgarcia.expensector.database.ExpensectorDatabase
import com.danielgarcia.expensector.domain.AppPreferencesRepository
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import com.danielgarcia.expensector.platform.Clock
import com.danielgarcia.expensector.platform.SystemClock
import com.danielgarcia.expensector.preferences.DataStoreAppPreferencesRepository
import com.danielgarcia.expensector.security.BiometricAvailabilityChecker
import com.danielgarcia.expensector.security.BiometricAvailabilityProvider
import com.danielgarcia.expensector.security.PinSecurityRepository
import com.danielgarcia.expensector.security.SharedPreferencesPinSecurityRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindLocalOwnerProfileRepository(
        repository: RoomLocalOwnerProfileRepository,
    ): LocalOwnerProfileRepository

    @Binds
    @Singleton
    abstract fun bindAppPreferencesRepository(
        repository: DataStoreAppPreferencesRepository,
    ): AppPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindPinSecurityRepository(
        repository: SharedPreferencesPinSecurityRepository,
    ): PinSecurityRepository
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpensectorDatabase =
        Room.databaseBuilder(context, ExpensectorDatabase::class.java, "expensector.db")
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()

    @Provides
    fun provideLocalOwnerProfileDao(database: ExpensectorDatabase) =
        database.localOwnerProfileDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    fun provideBiometricAvailabilityChecker(
        @ApplicationContext context: Context,
    ): BiometricAvailabilityProvider = BiometricAvailabilityChecker(context)
}
