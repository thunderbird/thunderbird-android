package app.k9mail.feature.preview.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.edit.navigation.accountEditRoute
import app.k9mail.feature.account.edit.navigation.navigateToAccountEditIncomingServerSettings
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.main.navigation.navigateToOnboarding
import app.k9mail.feature.onboarding.main.navigation.onboardingRoute

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
            onImport = { /* TODO */ },
        ) { accountUuid ->
            navController.navigateToAccountEditIncomingServerSettings(accountUuid)
        }
        accountSetupRoute(
            onBack = navController::popBackStack,
            onFinish = { accountUuid ->
                navController.navigateToAccountEditIncomingServerSettings(accountUuid)
            },
        )
        accountEditRoute(
            onBack = navController::popBackStack,
            onFinish = { navController.navigateToOnboarding() },
        )
    }
}
