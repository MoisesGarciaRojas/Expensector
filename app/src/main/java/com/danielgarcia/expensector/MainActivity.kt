package com.danielgarcia.expensector

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import com.danielgarcia.expensector.navigation.ExpensectorApp
import com.danielgarcia.expensector.security.BiometricPromptCoordinator
import com.danielgarcia.expensector.security.SecuritySessionViewModel
import com.danielgarcia.expensector.ui.theme.ExpensectorTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val securitySessionViewModel: SecuritySessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(securitySessionViewModel)
        val biometricPromptCoordinator = BiometricPromptCoordinator(this)

        setContent {
            ExpensectorTheme {
                val sessionState = securitySessionViewModel.state.collectAsStateWithLifecycle().value
                ExpensectorApp(
                    sessionState = sessionState,
                    biometricPromptCoordinator = biometricPromptCoordinator,
                    onAuthenticated = securitySessionViewModel::markAuthenticated,
                    onManualLock = securitySessionViewModel::manualLock,
                )
            }
        }
    }
}
