package net.thunderbird.feature.account.settings.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.account.settings.impl.ui.AccountSettingsScreen

class DefaultAccountSettingsNavigation : AccountSettingsNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (AccountSettingsRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<AccountSettingsRoute.GeneralSettings>(
                basePath = AccountSettingsRoute.GeneralSettings.Companion.BASE_PATH,
            ) { backStackEntry ->
                val generalSettingsRoute = backStackEntry.toRoute<AccountSettingsRoute.GeneralSettings>()

                AccountSettingsScreen(
                    accountId = generalSettingsRoute.accountId,
                    onBack = onBack,
                )
            }
        }
    }
}
