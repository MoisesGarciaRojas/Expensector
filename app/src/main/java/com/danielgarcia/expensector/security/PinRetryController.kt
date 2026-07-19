package com.danielgarcia.expensector.security

import com.danielgarcia.expensector.platform.Clock
import javax.inject.Inject

data class PinRetryState(
    val failedAttempts: Int = 0,
    val cooldownUntilEpochMillis: Long? = null,
) {
    fun isInCooldown(nowEpochMillis: Long): Boolean =
        cooldownUntilEpochMillis?.let { nowEpochMillis < it } == true
}

class PinRetryController @Inject constructor(
    private val clock: Clock,
) {
    companion object {
        const val MaxFailedAttempts = 5
        const val CooldownMillis = 30_000L
    }

    fun registerSuccess(): PinRetryState = PinRetryState()

    fun registerFailure(current: PinRetryState): PinRetryState {
        val now = clock.nowEpochMillis()
        if (current.isInCooldown(now)) return current
        val attempts = current.failedAttempts + 1
        return if (attempts >= MaxFailedAttempts) {
            PinRetryState(
                failedAttempts = attempts,
                cooldownUntilEpochMillis = now + CooldownMillis,
            )
        } else {
            current.copy(failedAttempts = attempts)
        }
    }
}
