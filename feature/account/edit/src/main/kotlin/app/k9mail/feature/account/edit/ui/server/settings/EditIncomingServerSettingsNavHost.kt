package app.k9mail.feature.account.edit.ui.server.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.edit.ui.server.settings.modify.ModifyIncomingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveIncomingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsScreen
import app.k9mail.feature.account.server.settings.R
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsScreen
import app.k9mail.feature.account.server.validation.ui.IncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val NESTED_NAVIGATION_ROUTE_MODIFY = "modify"
private const val NESTED_NAVIGATION_ROUTE_VALIDATE = "validate"
private const val NESTED_NAVIGATION_ROUTE_SAVE = "save"

private fun NavController.navigateToValidate() {
    navigate(NESTED_NAVIGATION_ROUTE_VALIDATE)
}

private fun NavController.navigateToSave() {
    navigate(NESTED_NAVIGATION_ROUTE_SAVE)
}

@Composable
fun EditIncomingServerSettingsNavHost(
    accountUuid: String,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_ROUTE_MODIFY,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_MODIFY) {
            IncomingServerSettingsScreen(
                onBack = onBack,
                onNext = { navController.navigateToValidate() },
                viewModel = koinViewModel<ModifyIncomingServerSettingsViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_VALIDATE) {
            ServerValidationScreen(
                title = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
                onBack = { navController.popBackStack() },
                onNext = { navController.navigateToSave() },
                viewModel = koinViewModel<IncomingServerValidationViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_SAVE) {
            SaveServerSettingsScreen(
                title = stringResource(id = R.string.account_server_settings_incoming_top_bar_title),
                onNext = onFinish,
                onBack = { navController.popBackStack(route = NESTED_NAVIGATION_ROUTE_MODIFY, inclusive = false) },
                viewModel = koinViewModel<SaveIncomingServerSettingsViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
    }
}
