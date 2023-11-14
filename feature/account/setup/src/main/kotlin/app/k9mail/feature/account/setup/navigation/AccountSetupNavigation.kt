package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable

const val NAVIGATION_ROUTE_ACCOUNT_SETUP = "account/setup"

fun NavGraphBuilder.accountSetupRoute(
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    deepLinkComposable(route = NAVIGATION_ROUTE_ACCOUNT_SETUP) {
        AccountSetupNavHost(
            onBack = onBack,
            onFinish = onFinish,
        )
    }
}
