package com.danielgarcia.expensector.security

import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.platform.Clock
import javax.inject.Inject

data class SecuritySessionState(
    val authenticated: Boolean = false,
    val locked: Boolean = true,
)

class SecuritySessionManager @Inject constructor(
    private val clock: Clock,
) {
    fun onProcessStart(onboardingCompleted: Boolean): SecuritySessionState =
        SecuritySessionState(
            authenticated = !onboardingCompleted,
            locked = onboardingCompleted,
        )

    fun shouldLockOnForeground(preferences: AppPreferences): Boolean {
        val backgroundAt = preferences.lastBackgroundAtEpochMillis ?: return true
        if (preferences.lockDuration == LockDuration.Immediately) return true
        return clock.nowEpochMillis() - backgroundAt >= preferences.lockDuration.timeoutMillis
    }
}
