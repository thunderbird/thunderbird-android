package app.k9mail.feature.account.setup.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute.AccountSetup

class DefaultAccountSetupNavigation : AccountSetupNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (AccountSetupRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<AccountSetup>(
                basePath = AccountSetup.BASE_PATH,
            ) {
                AccountSetupNavHost(
                    onBack = onBack,
                    onFinish = onFinish,
                )
            }
        }
    }
}
