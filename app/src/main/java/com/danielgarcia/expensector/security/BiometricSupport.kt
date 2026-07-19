package com.danielgarcia.expensector.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG

enum class BiometricAvailability {
    Available,
    NoHardware,
    HardwareUnavailable,
    NoneEnrolled,
    Unsupported,
    Unknown,
}

fun interface BiometricAvailabilityProvider {
    fun check(): BiometricAvailability
}

class BiometricAvailabilityChecker(
    private val context: Context,
) : BiometricAvailabilityProvider {
    override fun check(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricAvailability.Unsupported
            else -> BiometricAvailability.Unknown
        }
}
