package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import app.k9mail.feature.account.setup.ui.AccountSetupScreen

const val NAVIGATION_ROUTE_ACCOUNT_SETUP = "account_setup"

private const val NESTED_NAVIGATION_ROUTE_EMAIL = "email"

fun NavController.navigateToAccountSetup(navOptions: NavOptions? = null) {
    navigate(NAVIGATION_ROUTE_ACCOUNT_SETUP, navOptions)
}

fun NavGraphBuilder.accountSetupScreen(
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    navigation(
        route = NAVIGATION_ROUTE_ACCOUNT_SETUP,
        startDestination = NESTED_NAVIGATION_ROUTE_EMAIL,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_EMAIL) {
            AccountSetupScreen(
                onBackClick = onBackClick,
                onNextClick = onNextClick,
            )
        }
    }
}
