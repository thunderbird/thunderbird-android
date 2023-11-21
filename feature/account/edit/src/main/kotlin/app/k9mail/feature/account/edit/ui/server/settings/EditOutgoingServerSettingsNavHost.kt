package app.k9mail.feature.account.edit.ui.server.settings

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.edit.ui.server.settings.modify.ModifyOutgoingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveOutgoingServerSettingsViewModel
import app.k9mail.feature.account.edit.ui.server.settings.save.SaveServerSettingsScreen
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsScreen
import app.k9mail.feature.account.server.validation.ui.OutgoingServerValidationViewModel
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
fun EditOutgoingServerSettingsNavHost(
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
            OutgoingServerSettingsScreen(
                onBack = onBack,
                onNext = { navController.navigateToValidate() },
                viewModel = koinViewModel<ModifyOutgoingServerSettingsViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_VALIDATE) {
            ServerValidationScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigateToSave() },
                viewModel = koinViewModel<OutgoingServerValidationViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_SAVE) {
            SaveServerSettingsScreen(
                onNext = onFinish,
                onBack = { navController.popBackStack(route = NESTED_NAVIGATION_ROUTE_MODIFY, inclusive = false) },
                viewModel = koinViewModel<SaveOutgoingServerSettingsViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
    }
}
