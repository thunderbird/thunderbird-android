package app.k9mail.feature.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.core.ui.compose.common.activity.LocalActivity
import app.k9mail.feature.account.edit.navigation.accountEditRoute
import app.k9mail.feature.account.setup.navigation.accountSetupRoute
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.AccountSetupFinishedLauncher
import app.k9mail.feature.onboarding.main.navigation.NAVIGATION_ROUTE_ONBOARDING
import app.k9mail.feature.onboarding.main.navigation.onboardingRoute
import net.discdd.k9.onboarding.navigation.dddOnboardingRoute
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherNavHost(
    navController: NavHostController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accountSetupFinishedLauncher: AccountSetupFinishedLauncher = koinInject(),
) {
    val activity = LocalActivity.current

    NavHost(
        navController = navController,
        startDestination = NAVIGATION_ROUTE_ONBOARDING,
        modifier = modifier,
    ) {
        dddOnboardingRoute(
            onFinish = { accountUuid ->
                accountSetupFinishedLauncher.launch(accountUuid)
                activity.finish()
            }
        )
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
    }
}
