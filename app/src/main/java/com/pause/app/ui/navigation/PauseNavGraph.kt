package com.pause.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen
import com.pause.app.ui.onboarding.OnboardingViewModel

@Composable
fun PauseNavGraph() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsState(initial = false)

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete && navController.currentBackStackEntry?.destination?.route == "onboarding") {
            navController.navigate("home") {
                popUpTo("onboarding") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "onboarding"
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
                onNavigateToAppSelection = { navController.navigate("app_selection") }
            )
        }
        composable("app_selection") {
            OnboardingScreen(
                onComplete = { navController.popBackStack() },
                isAppSelectionOnly = true
            )
        }
    }
}
