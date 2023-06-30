package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import app.k9mail.feature.account.setup.ui.AccountSetupScreen

const val NAVIGATION_ROUTE_ACCOUNT_SETUP = "/account/setup"

fun NavController.navigateToAccountSetup(navOptions: NavOptions? = null) {
    navigate(NAVIGATION_ROUTE_ACCOUNT_SETUP, navOptions)
}

fun NavGraphBuilder.accountSetupRoute(
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    composable(route = NAVIGATION_ROUTE_ACCOUNT_SETUP) {
        AccountSetupScreen(
            onBack = onBack,
            onFinish = onFinish,
        )
    }
}
