package app.k9mail.feature.account.edit.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.k9mail.feature.account.edit.navigation.AccountEditRoute.ApiAccountReauthorization
import app.k9mail.feature.account.edit.navigation.AccountEditRoute.IncomingServerSettings
import app.k9mail.feature.account.edit.navigation.AccountEditRoute.OutgoingServerSettings
import app.k9mail.feature.account.edit.ui.server.settings.EditIncomingServerSettingsNavHost
import app.k9mail.feature.account.edit.ui.server.settings.EditOutgoingServerSettingsNavHost
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationScreen
import app.k9mail.feature.account.edit.ui.server.settings.reauthorize.ApiAccountReauthorizationViewModel
import net.thunderbird.core.ui.navigation.deepLinkComposable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class DefaultAccountEditNavigation : AccountEditNavigation {

    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (AccountEditRoute) -> Unit,
    ) = with(navGraphBuilder) {
        deepLinkComposable<ApiAccountReauthorization>(
            basePath = ApiAccountReauthorization.BASE_PATH,
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<ApiAccountReauthorization>()
            ApiAccountReauthorizationScreen(
                onBack = { onFinish(route) },
                viewModel = koinViewModel<ApiAccountReauthorizationViewModel> {
                    parametersOf(route.accountId)
                },
            )
        }

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
