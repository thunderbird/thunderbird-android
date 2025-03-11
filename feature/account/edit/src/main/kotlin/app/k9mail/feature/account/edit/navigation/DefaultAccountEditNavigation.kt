package app.k9mail.feature.account.edit.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import app.k9mail.feature.account.edit.navigation.AccountEditRoute.IncomingServerSettings
import app.k9mail.feature.account.edit.navigation.AccountEditRoute.OutgoingServerSettings
import app.k9mail.feature.account.edit.ui.server.settings.EditIncomingServerSettingsNavHost
import app.k9mail.feature.account.edit.ui.server.settings.EditOutgoingServerSettingsNavHost

class DefaultAccountEditNavigation : AccountEditNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (AccountEditRoute) -> Unit,
    ) = with(navGraphBuilder) {
        deepLinkComposable<IncomingServerSettings>(
            basePath = IncomingServerSettings.BASE_PATH,
        ) { backStackEntry ->
            val incomingServerSettingsRoute = backStackEntry.toRoute<IncomingServerSettings>()

            EditIncomingServerSettingsNavHost(
                accountUuid = incomingServerSettingsRoute.accountId,
                onBack = onBack,
                onFinish = onFinish,
            )
        }

        deepLinkComposable<OutgoingServerSettings>(
            basePath = OutgoingServerSettings.BASE_PATH,
        ) { backStackEntry ->
            val outgoingServerSettingsRoute = backStackEntry.toRoute<OutgoingServerSettings>()

            EditOutgoingServerSettingsNavHost(
                accountUuid = outgoingServerSettingsRoute.accountId,
                onBack = onBack,
                onFinish = onFinish,
            )
        }
    }
}
