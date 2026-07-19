package com.danielgarcia.expensector.security

import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data object Failed : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Error(val code: Int) : BiometricAuthResult
}

class BiometricPromptCoordinator(
    private val activity: FragmentActivity,
) {
    fun authenticate(onResult: (BiometricAuthResult) -> Unit) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(BiometricAuthResult.Success)
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricAuthResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val result = when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> BiometricAuthResult.Cancelled
                        else -> BiometricAuthResult.Error(errorCode)
                    }
                    onResult(result)
                }
            },
        )
        prompt.authenticate(
            PromptInfo.Builder()
                .setTitle(activity.getString(com.danielgarcia.expensector.R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(com.danielgarcia.expensector.R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(activity.getString(com.danielgarcia.expensector.R.string.use_pin_instead))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build(),
        )
    }
}
