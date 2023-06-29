package app.k9mail.feature.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountSetupFinishedLauncher
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.account.setup.navigation.navigateToAccountSetup
import app.k9mail.feature.onboarding.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.navigation.onboardingRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherNavHost(
    navController: NavHostController,
    startDestination: String?,
    modifier: Modifier = Modifier,
    accountSetupFinishedLauncher: AccountSetupFinishedLauncher = koinInject<AccountSetupFinishedLauncher>(),
) {
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination ?: NAVIGATION_ROUTE_ONBOARDING,
        modifier = modifier,
    ) {
        onboardingRoute(
            onStart = { navController.navigateToAccountSetup() },
            onImport = { /* TODO */ },
        )
        accountSetupRoute(
            onBack = navController::popBackStack,
            onFinish = {
                coroutineScope.launch {
                    accountSetupFinishedLauncher.launch(it)
                }
            },
        )
    }
}
