package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable
import app.k9mail.feature.account.setup.ui.AccountSetupScreen

const val NAVIGATION_ROUTE_ACCOUNT_SETUP = "account/setup"

fun NavController.navigateToAccountSetup(navOptions: NavOptions? = null) {
    navigate(NAVIGATION_ROUTE_ACCOUNT_SETUP, navOptions)
}

fun NavGraphBuilder.accountSetupRoute(
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    deepLinkComposable(route = NAVIGATION_ROUTE_ACCOUNT_SETUP) {
        AccountSetupScreen(
            onBack = onBack,
            onFinish = onFinish,
        )
    }
}
