package app.k9mail.feature.preview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.setup.navigation.accountSetupScreen
import app.k9mail.feature.account.setup.navigation.navigateToAccountSetup
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.navigation.onboardingScreen

@Composable
fun FeatureNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NAVIGATION_ROUTE_ONBOARDING,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        onboardingScreen(
            onStartClick = { navController.navigateToAccountSetup() },
            onImportClick = { /* TODO */ },
        )
        accountSetupScreen(
            onBackClick = navController::popBackStack,
            onFinishClick = { /* TODO */ },
        )
    }
}
