package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import app.k9mail.feature.account.setup.ui.AccountSetupScreen

const val NAVIGATION_ROUTE_ACCOUNT_SETUP = "/account/setup"
const val DEEPLINK_ACCOUNT_SETUP = "app://$NAVIGATION_ROUTE_ACCOUNT_SETUP"

fun NavController.navigateToAccountSetup(navOptions: NavOptions? = null) {
    navigate(NAVIGATION_ROUTE_ACCOUNT_SETUP, navOptions)
}

fun NavGraphBuilder.accountSetupRoute(
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    composable(
        route = NAVIGATION_ROUTE_ACCOUNT_SETUP,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEPLINK_ACCOUNT_SETUP },
        ),
    ) {
        AccountSetupScreen(
            onBack = onBack,
            onFinish = onFinish,
        )
    }
}
