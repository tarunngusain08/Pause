package com.pause.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pause.app.ui.appselection.AppSelectionScreen
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen
import com.pause.app.ui.onboarding.OnboardingViewModel
import com.pause.app.ui.strict.StrictModeSetupScreen
import com.pause.app.ui.parental.ParentalSetupScreen
import com.pause.app.ui.parental.ParentDashboardScreen
import com.pause.app.ui.parental.ChildStatusScreen

@Composable
fun PauseNavGraph() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isReady by onboardingViewModel.isReady.collectAsState()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState()

    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Pause",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val startDestination = if (onboardingComplete) "home" else "onboarding"
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onNavigateToAppSelection = { navController.navigate("app_selection") },
                onNavigateToStrictSetup = { navController.navigate("strict_setup") },
                onNavigateToParentalSetup = { navController.navigate("parental_setup") }
            )
        }
        composable("strict_setup") {
            StrictModeSetupScreen(
                onSessionStarted = {
                    navController.navigate("home") {
                        popUpTo("strict_setup") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("app_selection") {
            AppSelectionScreen(
                onDone = { navController.popBackStack() }
            )
        }
        composable("parental_setup") {
            ParentalSetupScreen(
                onComplete = {
                    navController.navigate("parent_dashboard") {
                        popUpTo("parental_setup") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("parent_dashboard") {
            ParentDashboardScreen(onBack = { navController.popBackStack() })
        }
        composable("child_status") {
            ChildStatusScreen()
        }
    }
}
