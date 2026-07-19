package com.danielgarcia.expensector.feature.settings

import com.danielgarcia.expensector.FakeClock
import com.danielgarcia.expensector.FakePinSecurityRepository
import com.danielgarcia.expensector.FakePreferencesRepository
import com.danielgarcia.expensector.FakeProfileRepository
import com.danielgarcia.expensector.MainDispatcherRule
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LockDuration
import com.danielgarcia.expensector.security.BiometricAvailability
import com.danielgarcia.expensector.security.BiometricAvailabilityProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun biometricPreferenceUpdatesAfterCurrentPin() {
        val fixture = fixture()
        fixture.viewModel.updateCurrentPin("123456")
        var invalidated = false

        fixture.viewModel.setBiometricEnabled(true) { invalidated = true }

        assertEquals(true, fixture.preferences.preferences.value.biometricEnabled)
        assertEquals(true, invalidated)
    }

    @Test
    fun lockDurationUpdatesAfterCurrentPin() {
        val fixture = fixture()
        fixture.viewModel.updateCurrentPin("123456")

        fixture.viewModel.setLockDuration(LockDuration.FiveMinutes) {}

        assertEquals(LockDuration.FiveMinutes, fixture.preferences.preferences.value.lockDuration)
    }

    @Test
    fun pinChangeRequiresValidCurrentAuthentication() {
        val fixture = fixture()
        fixture.pinRepository.verifyResult = com.danielgarcia.expensector.security.PinVerificationResult.InvalidPin
        fixture.viewModel.updateCurrentPin("000000")
        fixture.viewModel.updateNewPin("654321")

        fixture.viewModel.changePin("654321") {}

        assertEquals("123456", fixture.pinRepository.pin)
        assertEquals(SettingsError.InvalidPin, fixture.viewModel.state.value.error)
    }

    @Test
    fun saveProfilePersistsDisplayNameAndEmail() {
        val fixture = fixture()
        fixture.viewModel.updateDisplayName("Daniel G")
        fixture.viewModel.updateEmail("daniel@example.com")

        fixture.viewModel.saveProfile()

        assertEquals("Daniel G", fixture.profileRepository.profile.value?.displayName)
        assertEquals("daniel@example.com", fixture.profileRepository.profile.value?.email)
    }

    private fun fixture(): Fixture {
        val profileRepository = FakeProfileRepository()
        profileRepository.profile.value = LocalOwnerProfile(
            id = "owner",
            displayName = "Daniel",
            email = null,
            defaultCurrencyCode = "MXN",
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
        val preferencesRepository = FakePreferencesRepository()
        val pinRepository = FakePinSecurityRepository().apply { pin = "123456" }
        val viewModel = SettingsViewModel(
            profileRepository,
            preferencesRepository,
            pinRepository,
            BiometricAvailabilityProvider { BiometricAvailability.Available },
            FakeClock(now = 2L),
        )
        return Fixture(viewModel, profileRepository, preferencesRepository, pinRepository)
    }

    private data class Fixture(
        val viewModel: SettingsViewModel,
        val profileRepository: FakeProfileRepository,
        val preferences: FakePreferencesRepository,
        val pinRepository: FakePinSecurityRepository,
    )
}
