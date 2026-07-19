package com.danielgarcia.expensector.domain

data class AppPreferences(
    val onboardingCompleted: Boolean,
    val biometricEnabled: Boolean,
    val lockDuration: LockDuration,
    val lastBackgroundAtEpochMillis: Long?,
)
