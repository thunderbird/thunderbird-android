package app.k9mail.feature.account.edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import app.k9mail.core.ui.compose.common.navigation.deepLinkComposable
import app.k9mail.core.ui.compose.common.navigation.getStringArgument
import app.k9mail.feature.account.edit.ui.server.settings.EditIncomingServerSettingsNavHost
import app.k9mail.feature.account.edit.ui.server.settings.EditOutgoingServerSettingsNavHost

internal const val ARGUMENT_ACCOUNT_UUID = "accountUuid"

const val NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING = "account/edit/server/settings/incoming/{accountUuid}"
const val NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING = "account/edit/server/settings/outgoing/{accountUuid}"

fun NavController.navigateToAccountEditIncomingServerSettings(accountUuid: String) {
    navigate(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING.withAccountUuid(accountUuid),
    )
}

fun NavController.navigateToAccountEditOutgoingServerSettings(accountUuid: String) {
    navigate(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING.withAccountUuid(accountUuid),
    )
}

fun NavGraphBuilder.accountEditRoute(
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    deepLinkComposable(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_INCOMING,
        arguments = listOf(
            navArgument(ARGUMENT_ACCOUNT_UUID) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val accountUuid = backStackEntry.getStringArgument(ARGUMENT_ACCOUNT_UUID)
        EditIncomingServerSettingsNavHost(
            accountUuid = accountUuid,
            onFinish = { onFinish() },
            onBack = onBack,
        )
    }
    deepLinkComposable(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_SERVER_SETTINGS_OUTGOING,
        arguments = listOf(
            navArgument(ARGUMENT_ACCOUNT_UUID) {
                type = NavType.StringType
            },
        ),
    ) { backStackEntry ->
        val accountUuid = backStackEntry.getStringArgument(ARGUMENT_ACCOUNT_UUID)
        EditOutgoingServerSettingsNavHost(
            accountUuid = accountUuid,
            onFinish = { onFinish() },
            onBack = onBack,
        )
    }
}

fun String.withAccountUuid(accountUuid: String): String {
    return replace(
        oldValue = "{$ARGUMENT_ACCOUNT_UUID}",
        newValue = accountUuid,
    )
}
