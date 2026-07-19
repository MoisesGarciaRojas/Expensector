package com.danielgarcia.expensector.security

import com.danielgarcia.expensector.FakeClock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {
    @Test
    fun pinPolicyAcceptsExactlySixNumericDigits() {
        assertTrue(PinPolicy.isValid("123456"))
        assertFalse(PinPolicy.isValid("12345"))
        assertFalse(PinPolicy.isValid("1234567"))
        assertFalse(PinPolicy.isValid("12345a"))
    }

    @Test
    fun verifierValidatesCorrectPinAndRejectsIncorrectPin() {
        val service = Pbkdf2PinVerifierService()
        val verifier = service.createVerifier("123456")

        assertTrue(service.verify("123456", verifier))
        assertFalse(service.verify("654321", verifier))
    }

    @Test
    fun retryControllerActivatesCooldownAfterFiveFailures() {
        val clock = FakeClock(now = 1_000L)
        val controller = PinRetryController(clock)
        var state = PinRetryState()

        repeat(4) {
            state = controller.registerFailure(state)
            assertFalse(state.isInCooldown(clock.nowEpochMillis()))
        }

        state = controller.registerFailure(state)
        assertTrue(state.isInCooldown(clock.nowEpochMillis()))

        clock.now += PinRetryController.CooldownMillis + 1
        assertFalse(state.isInCooldown(clock.nowEpochMillis()))
    }
}
