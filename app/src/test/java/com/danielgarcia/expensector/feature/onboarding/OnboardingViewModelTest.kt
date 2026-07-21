package com.danielgarcia.expensector.feature.onboarding

import com.danielgarcia.expensector.FakeClock
import com.danielgarcia.expensector.FakePinSecurityRepository
import com.danielgarcia.expensector.FakePreferencesRepository
import com.danielgarcia.expensector.FakeProfileRepository
import com.danielgarcia.expensector.FakeStage2Repository
import com.danielgarcia.expensector.MainDispatcherRule
import com.danielgarcia.expensector.security.BiometricAvailability
import com.danielgarcia.expensector.security.BiometricAvailabilityProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun incompleteOnboardingStartsAtWelcome() {
        val viewModel = viewModel()

        assertEquals(OnboardingStep.Welcome, viewModel.state.value.step)
        assertFalse(viewModel.preferences.preferences.value.onboardingCompleted)
    }

    @Test
    fun onboardingCannotCompleteWithoutValidPin() {
        val viewModel = viewModel()
        viewModel.next()
        viewModel.updateDisplayName("Daniel")
        viewModel.next()
        viewModel.next()
        viewModel.updatePin("123")
        viewModel.next()

        assertEquals(OnboardingError.InvalidPin, viewModel.state.value.error)
        assertFalse(viewModel.preferences.preferences.value.onboardingCompleted)
    }

    @Test
    fun confirmationMismatchIsReported() {
        val viewModel = viewModel()
        viewModel.next()
        viewModel.updateDisplayName("Daniel")
        viewModel.next()
        viewModel.next()
        viewModel.updatePin("123456")
        viewModel.next()
        viewModel.updateConfirmPin("654321")
        viewModel.next()

        assertEquals(OnboardingError.PinMismatch, viewModel.state.value.error)
    }

    @Test
    fun completedOnboardingPersistsProfilePinAndPreference() {
        val viewModel = viewModel()
        viewModel.next()
        viewModel.updateDisplayName("Daniel")
        viewModel.updateEmail("daniel@example.com")
        viewModel.next()
        viewModel.next()
        viewModel.updatePin("123456")
        viewModel.next()
        viewModel.updateConfirmPin("123456")
        viewModel.next()
        viewModel.skipBiometrics()

        assertEquals(OnboardingStep.Complete, viewModel.state.value.step)
        assertEquals("123456", viewModel.pinRepository.pin)
        assertEquals(true, viewModel.preferences.preferences.value.onboardingCompleted)
        assertNotNull(viewModel.profileRepository.profile.value)
    }

    private fun viewModel(): TestOnboardingViewModel {
        val profileRepository = FakeProfileRepository()
        val preferencesRepository = FakePreferencesRepository()
        val pinRepository = FakePinSecurityRepository()
        val stage2Repository = FakeStage2Repository()
        val clock = FakeClock(now = 100L)
        return TestOnboardingViewModel(
            profileRepository = profileRepository,
            preferences = preferencesRepository,
            pinRepository = pinRepository,
            delegate = OnboardingViewModel(
                profileRepository,
                preferencesRepository,
                stage2Repository,
                pinRepository,
                BiometricAvailabilityProvider { BiometricAvailability.Available },
                clock,
            ),
        )
    }
}

private class TestOnboardingViewModel(
    val profileRepository: FakeProfileRepository,
    val preferences: FakePreferencesRepository,
    val pinRepository: FakePinSecurityRepository,
    private val delegate: OnboardingViewModel,
) {
    val state get() = delegate.state
    fun updateDisplayName(value: String) = delegate.updateDisplayName(value)
    fun updateEmail(value: String) = delegate.updateEmail(value)
    fun updatePin(value: String) = delegate.updatePin(value)
    fun updateConfirmPin(value: String) = delegate.updateConfirmPin(value)
    fun next() = delegate.next()
    fun skipBiometrics() = delegate.skipBiometrics()
}
