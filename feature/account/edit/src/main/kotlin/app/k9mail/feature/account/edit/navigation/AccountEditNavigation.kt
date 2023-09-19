package app.k9mail.feature.account.edit.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import app.k9mail.core.ui.compose.common.navigation.getArgument
import app.k9mail.feature.account.edit.ui.EditIncomingServerSettingsNavHost
import app.k9mail.feature.account.edit.ui.EditOutgoingServerSettingsNavHost
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf

internal const val ARGUMENT_ACCOUNT_UUID = "accountUuid"

const val NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_INCOMING = "/account/edit/config/incoming/{accountUuid}"
const val NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_OUTGOING = "/account/edit/config/outgoing/{accountUuid}"

fun NavController.navigateToAccountEditConfigIncoming(accountUuid: String) {
    navigate(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_INCOMING.withAccountUuid(accountUuid),
    )
}

fun NavController.navigateToAccountEditConfigOutgoing(accountUuid: String) {
    navigate(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_OUTGOING.withAccountUuid(accountUuid),
    )
}

fun NavGraphBuilder.accountEditRoute(
    startDestinationArguments: ImmutableMap<String, String> = persistentMapOf(),
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    composable(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_INCOMING,
        arguments = listOf(
            navArgument(ARGUMENT_ACCOUNT_UUID) {
                type = NavType.StringType
                defaultValue = startDestinationArguments[ARGUMENT_ACCOUNT_UUID] ?: ""
            },
        ),
    ) { backStackEntry ->
        val accountUuid = backStackEntry.getArgument(ARGUMENT_ACCOUNT_UUID)
        EditIncomingServerSettingsNavHost(
            accountUuid = accountUuid,
            onFinish = { onFinish() },
            onBack = onBack,
        )
    }
    composable(
        route = NAVIGATION_ROUTE_ACCOUNT_EDIT_CONFIG_OUTGOING,
        arguments = listOf(
            navArgument(ARGUMENT_ACCOUNT_UUID) {
                type = NavType.StringType
                defaultValue = startDestinationArguments[ARGUMENT_ACCOUNT_UUID] ?: ""
            },
        ),
    ) { backStackEntry ->
        val accountUuid = backStackEntry.getArgument(ARGUMENT_ACCOUNT_UUID)
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
