package app.k9mail.feature.launcher.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import app.k9mail.feature.account.edit.navigation.accountEditRoute
import app.k9mail.feature.account.setup.navigation.AccountSetupNavigation
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.funding.api.FundingNavigation
import app.k9mail.feature.launcher.FeatureLauncherExternalContract.AccountSetupFinishedLauncher
import app.k9mail.feature.onboarding.main.navigation.OnboardingNavigation
import app.k9mail.feature.onboarding.main.navigation.OnboardingRoute
import org.koin.compose.koinInject

@Composable
fun FeatureLauncherNavHost(
    navController: NavHostController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    accountSetupFinishedLauncher: AccountSetupFinishedLauncher = koinInject(),
    accountSetupNavigation: AccountSetupNavigation = koinInject(),
    onboardingNavigation: OnboardingNavigation = koinInject(),
    fundingNavigation: FundingNavigation = koinInject(),
) {
    val activity = LocalActivity.current as ComponentActivity

    NavHost(
        navController = navController,
        startDestination = OnboardingRoute.Onboarding(),
        modifier = modifier,
    ) {
        onboardingNavigation.registerRoutes(
            navGraphBuilder = this,
            onBack = onBack,
            onFinish = {
                when (it) {
                    is OnboardingRoute.Onboarding -> {
                        accountSetupFinishedLauncher.launch(it.accountId)
                        activity.finish()
                    }
                }
            },
        )

        accountSetupNavigation.registerRoutes(
            navGraphBuilder = this,
            onBack = onBack,
            onFinish = {
                when (it) {
                    is AccountSetupRoute.AccountSetup -> {
                        accountSetupFinishedLauncher.launch(it.accountId)
                    }
                }
            },
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
