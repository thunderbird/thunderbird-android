package app.k9mail.feature.preview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.account.setup.navigation.navigateToAccountSetup
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.navigation.navigateToOnboarding
import app.k9mail.feature.onboarding.navigation.onboardingRoute

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
        onboardingRoute(
            onStart = { navController.navigateToAccountSetup() },
            onImport = { /* TODO */ },
        )
        accountSetupRoute(
            onBack = navController::popBackStack,
            onFinish = { navController.navigateToOnboarding() },
        )
    }
}
