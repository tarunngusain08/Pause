package com.pause.app.ui.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pause.app.ui.appselection.AppSelectionScreen
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen
import com.pause.app.ui.focus.FocusModeScreen
import com.pause.app.ui.insights.WeeklyInsightsScreen
import com.pause.app.ui.settings.SettingsScreen
import com.pause.app.ui.onboarding.OnboardingViewModel
import com.pause.app.ui.strict.StrictModeSetupScreen
import com.pause.app.ui.webfilter.DomainBlacklistScreen
import com.pause.app.ui.webfilter.KeywordManagerScreen
import com.pause.app.ui.webfilter.UnblockRequestScreen
import com.pause.app.ui.webfilter.UrlVisitLogScreen
import com.pause.app.ui.webfilter.WebFilterDashboardScreen
import com.pause.app.ui.webfilter.WhitelistScreen

@Composable
fun PauseNavGraph() {
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val isReady by onboardingViewModel.isReady.collectAsStateWithLifecycle()
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle()

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
                onNavigateToAppSelection = { navController.navigate(Routes.APP_SELECTION) },
                onNavigateToStrictSetup = { navController.navigate(Routes.STRICT_SETUP) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToFocus = { navController.navigate(Routes.FOCUS) },
                onNavigateToInsights = { navController.navigate(Routes.WEEKLY_INSIGHTS) },
                onNavigateToContentShield = { navController.navigate(Routes.CONTENT_SHIELD) }
            )
        }
        composable(Routes.WEEKLY_INSIGHTS) {
            WeeklyInsightsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FOCUS) {
            FocusModeScreen(
                onSessionStarted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.FOCUS) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STRICT_SETUP) {
            StrictModeSetupScreen(
                onSessionStarted = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.STRICT_SETUP) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.APP_SELECTION) {
            AppSelectionScreen(
                onDone = { navController.popBackStack() }
            )
        }
        composable(Routes.CONTENT_SHIELD) {
            WebFilterDashboardScreen(
                onNavigateToBlacklist = { navController.navigate(Routes.DOMAIN_BLACKLIST) },
                onNavigateToKeywords = { navController.navigate(Routes.KEYWORD_MANAGER) },
                onNavigateToUrlLog = { navController.navigate(Routes.URL_VISIT_LOG) },
                onNavigateToWhitelist = { navController.navigate(Routes.WHITELIST) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.WEB_FILTER_DASHBOARD) {
            WebFilterDashboardScreen(
                onNavigateToBlacklist = { navController.navigate(Routes.DOMAIN_BLACKLIST) },
                onNavigateToKeywords = { navController.navigate(Routes.KEYWORD_MANAGER) },
                onNavigateToUrlLog = { navController.navigate(Routes.URL_VISIT_LOG) },
                onNavigateToWhitelist = { navController.navigate(Routes.WHITELIST) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DOMAIN_BLACKLIST) {
            DomainBlacklistScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.KEYWORD_MANAGER) {
            KeywordManagerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.URL_VISIT_LOG) {
            UrlVisitLogScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.WHITELIST) {
            WhitelistScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.UNBLOCK_REQUEST,
            arguments = listOf(navArgument("domain") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "pause://unblock-request?domain={domain}" }
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
