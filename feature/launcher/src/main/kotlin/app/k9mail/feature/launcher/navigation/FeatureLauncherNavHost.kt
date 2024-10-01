package app.k9mail.feature.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.feature.account.edit.navigation.accountEditRoute
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.AccountSetupFinishedLauncher
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.main.navigation.onboardingRoute
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherNavHost(
    navController: NavHostController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accountSetupFinishedLauncher: AccountSetupFinishedLauncher = koinInject(),
    fundingNavigation: FundingNavigation = koinInject(),
) {
    val activity = LocalActivity.current

    NavHost(
        navController = navController,
        startDestination = NAVIGATION_ROUTE_ONBOARDING,
        modifier = modifier,
    ) {
        onboardingRoute(
            onFinish = { accountUuid ->
                accountSetupFinishedLauncher.launch(accountUuid)
                activity.finish()
            },
        )
        accountSetupRoute(
            onBack = onBack,
            onFinish = { accountSetupFinishedLauncher.launch(it) },
        )
        accountEditRoute(
            onBack = onBack,
            onFinish = { activity.finish() },
        )

        fundingNavigation.registerRoutes(
            navGraphBuilder = this,
            onBack = onBack,
            onFinish = { onBack() },
        )
    }
}
