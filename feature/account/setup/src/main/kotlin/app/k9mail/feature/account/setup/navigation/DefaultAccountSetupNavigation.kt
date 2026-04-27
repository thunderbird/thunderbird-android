package app.k9mail.feature.account.setup.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.navigation.NavGraphBuilder
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute.AccountSetup
import net.thunderbird.core.ui.navigation.deepLinkComposable

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
                SharedTransitionLayout {
                    AccountSetupNavHost(
                        onBack = onBack,
                        onFinish = onFinish,
                    )
                }
            }
        }
    }
}
