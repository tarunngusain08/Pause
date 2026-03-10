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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.pause.app.ui.appselection.AppSelectionScreen
import com.pause.app.ui.home.HomeScreen
import com.pause.app.ui.onboarding.OnboardingScreen
import com.pause.app.ui.commitment.CommitmentModeScreen
import com.pause.app.ui.focus.FocusModeScreen
import com.pause.app.ui.insights.WeeklyInsightsScreen
import com.pause.app.ui.settings.SettingsScreen
import com.pause.app.ui.onboarding.OnboardingViewModel
import com.pause.app.ui.strict.StrictModeSetupScreen
import com.pause.app.ui.parental.ParentalSetupScreen
import com.pause.app.ui.parental.ParentDashboardScreen
import com.pause.app.ui.parental.ChildStatusScreen
import com.pause.app.ui.webfilter.DomainBlacklistScreen
import com.pause.app.ui.webfilter.KeywordManagerScreen
import com.pause.app.ui.webfilter.UnblockRequestScreen
import com.pause.app.ui.webfilter.UrlVisitLogScreen
import com.pause.app.ui.webfilter.WebFilterDashboardScreen
import com.pause.app.ui.webfilter.WhitelistScreen

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
                onNavigateToParentalSetup = { navController.navigate("parental_setup") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToFocus = { navController.navigate("focus") },
                onNavigateToInsights = { navController.navigate("weekly_insights") },
                onNavigateToCommitment = { navController.navigate("commitment_setup") },
                onNavigateToChildStatus = { navController.navigate("child_status") }
            )
        }
        composable("commitment_setup") {
            CommitmentModeScreen(
                onSessionStarted = {
                    navController.navigate("home") {
                        popUpTo("commitment_setup") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("weekly_insights") {
            WeeklyInsightsScreen(onBack = { navController.popBackStack() })
        }
        composable("focus") {
            FocusModeScreen(
                onSessionStarted = {
                    navController.navigate("home") {
                        popUpTo("focus") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
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
            ParentDashboardScreen(
                onBack = { navController.popBackStack() },
                onNavigateToWebFilter = { navController.navigate("web_filter_dashboard") }
            )
        }
        composable("child_status") {
            ChildStatusScreen(onBack = { navController.popBackStack() })
        }
        composable("web_filter_dashboard") {
            WebFilterDashboardScreen(
                onNavigateToBlacklist = { navController.navigate("domain_blacklist") },
                onNavigateToKeywords = { navController.navigate("keyword_manager") },
                onNavigateToUrlLog = { navController.navigate("url_visit_log") },
                onNavigateToWhitelist = { navController.navigate("whitelist") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("domain_blacklist") {
            DomainBlacklistScreen(onBack = { navController.popBackStack() })
        }
        composable("keyword_manager") {
            KeywordManagerScreen(onBack = { navController.popBackStack() })
        }
        composable("url_visit_log") {
            UrlVisitLogScreen(onBack = { navController.popBackStack() })
        }
        composable("whitelist") {
            WhitelistScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "unblock_request/{domain}",
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
