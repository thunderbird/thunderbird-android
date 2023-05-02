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

fun NavGraphBuilder.accountSetupScreen(
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    composable(route = NAVIGATION_ROUTE_ACCOUNT_SETUP) {
        AccountSetupScreen(
            onBackClick = onBackClick,
            onFinishClick = onFinishClick,
        )
    }
}
