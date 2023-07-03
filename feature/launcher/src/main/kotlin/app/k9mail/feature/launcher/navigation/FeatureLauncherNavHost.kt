package app.k9mail.feature.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.account.setup.navigation.navigateToAccountSetup
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.AccountSetupFinishedLauncher
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.ImportSettingsLauncher
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.navigation.onboardingRoute
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherNavHost(
    navController: NavHostController,
    startDestination: String?,
    modifier: Modifier = Modifier,
    importSettingsLauncher: ImportSettingsLauncher = koinInject(),
    accountSetupFinishedLauncher: AccountSetupFinishedLauncher = koinInject(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination ?: NAVIGATION_ROUTE_ONBOARDING,
        modifier = modifier,
    ) {
        onboardingRoute(
            onStart = { navController.navigateToAccountSetup() },
            onImport = { importSettingsLauncher.launch() },
        )
        accountSetupRoute(
            onBack = navController::popBackStack,
            onFinish = { accountSetupFinishedLauncher.launch(it) },
        )
    }
}
