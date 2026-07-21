package com.danielgarcia.expensector.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.danielgarcia.expensector.R
import com.danielgarcia.expensector.core.ui.FoundationScreen
import com.danielgarcia.expensector.feature.authentication.LockScreen
import com.danielgarcia.expensector.feature.home.HomeScreen
import com.danielgarcia.expensector.feature.financial.FinancialSetupScreen
import com.danielgarcia.expensector.feature.onboarding.OnboardingScreen
import com.danielgarcia.expensector.feature.settings.SettingsScreen
import com.danielgarcia.expensector.security.BiometricAvailabilityChecker
import com.danielgarcia.expensector.security.BiometricPromptCoordinator
import com.danielgarcia.expensector.security.SecuritySessionState

private object Routes {
    const val Home = "home"
    const val Settings = "settings"
    const val FinancialSetup = "financial_setup"
}

@Composable
fun ExpensectorApp(
    sessionState: SecuritySessionState,
    biometricPromptCoordinator: BiometricPromptCoordinator,
    onAuthenticated: () -> Unit,
    onManualLock: () -> Unit,
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val biometricAvailabilityChecker = remember(context) { BiometricAvailabilityChecker(context) }
    val rootState = rootViewModel.state.collectAsStateWithLifecycle().value
    when {
        rootState.destination == RootDestination.Loading -> FoundationScreen(title = stringResource(R.string.app_name)) {
            CircularProgressIndicator()
        }
        rootState.destination == RootDestination.Onboarding -> OnboardingScreen(onCompleted = onAuthenticated)
        sessionState.authenticated -> AuthenticatedNav(onManualLock)
        else -> LockScreen(
            biometricEnabled = rootState.preferences.biometricEnabled,
            biometricAvailability = biometricAvailabilityChecker.check(),
            biometricPromptCoordinator = biometricPromptCoordinator,
            onAuthenticated = onAuthenticated,
        )
    }
}

@Composable
private fun AuthenticatedNav(onManualLock: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.Home) {
        composable(Routes.Home) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenFinancialSetup = { navController.navigate(Routes.FinancialSetup) },
                onManualLock = onManualLock,
            )
        }
        composable(Routes.FinancialSetup) {
            FinancialSetupScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onSecurityInvalidated = onManualLock,
            )
        }
    }
}
