package net.thunderbird.feature.account.settings.impl

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.settings.api.AccountSettingsNavigation
import net.thunderbird.feature.account.settings.api.AccountSettingsRoute
import net.thunderbird.feature.account.settings.impl.ui.general.GeneralSettingsScreen

internal class DefaultAccountSettingsNavigation : AccountSettingsNavigation {

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
                val accountId = AccountIdFactory.of(generalSettingsRoute.accountId)

                GeneralSettingsScreen(
                    accountId = accountId,
                    onBack = onBack,
                )
            }
        }
    }
}
