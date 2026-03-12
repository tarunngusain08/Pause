package com.pause.app.ui.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen
import com.pause.app.ui.onboarding.OnboardingViewModel
import com.pause.app.ui.strict.StrictModeSetupScreen
import com.pause.app.ui.contentshield.ContentShieldScreen
import com.pause.app.ui.webfilter.UnblockRequestScreen

@Composable
fun PauseNavGraph() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isReady by onboardingViewModel.isReady.collectAsStateWithLifecycle()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle()

    if (!isReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Focus",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        return
    }

    val startDestination = if (onboardingComplete) Routes.HOME else Routes.ONBOARDING
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToStrictSetup = { navController.navigate(Routes.FOCUS_SETUP) },
                onNavigateToContentShield = { navController.navigate(Routes.CONTENT_SHIELD) }
            )
        }
        composable(Routes.FOCUS_SETUP) {
            StrictModeSetupScreen(
                onSessionStarted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.FOCUS_SETUP) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.CONTENT_SHIELD) {
            ContentShieldScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.UNBLOCK_REQUEST,
            arguments = listOf(navArgument("domain") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "focus://unblock-request?domain={domain}" }
            )
        ) { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain") ?: ""
            UnblockRequestScreen(
                domain = domain,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
