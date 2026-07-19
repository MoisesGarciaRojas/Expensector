package com.danielgarcia.expensector.platform

import com.danielgarcia.expensector.FakeClock
import com.danielgarcia.expensector.domain.AppPreferences
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.security.SecuritySessionManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecuritySessionManagerTest {
    @Test
    fun remainsUnlockedBeforeTimeoutAndLocksAtTimeout() {
        val clock = FakeClock(now = 59_999L)
        val manager = SecuritySessionManager(clock)
        val preferences = AppPreferences(true, false, LockDuration.OneMinute, lastBackgroundAtEpochMillis = 0L)

        assertFalse(manager.shouldLockOnForeground(preferences))

        clock.now = 60_000L
        assertTrue(manager.shouldLockOnForeground(preferences))
    }

    @Test
    fun immediateLockOptionAlwaysLocks() {
        val manager = SecuritySessionManager(FakeClock(now = 1L))
        val preferences = AppPreferences(true, false, LockDuration.Immediately, lastBackgroundAtEpochMillis = 1L)

        assertTrue(manager.shouldLockOnForeground(preferences))
    }

    @Test
    fun processStartLocksCompletedOnboarding() {
        val manager = SecuritySessionManager(FakeClock())

        assertTrue(manager.onProcessStart(onboardingCompleted = true).locked)
        assertFalse(manager.onProcessStart(onboardingCompleted = false).locked)
    }
}
