package app.k9mail.feature.account.edit.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsScreen
import app.k9mail.feature.account.server.validation.ui.IncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val NESTED_NAVIGATION_ROUTE_CONFIG = "config"
private const val NESTED_NAVIGATION_ROUTE_VALIDATION = "validation"

private fun NavController.navigateToValidation() {
    navigate(NESTED_NAVIGATION_ROUTE_VALIDATION)
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
        startDestination = NESTED_NAVIGATION_ROUTE_CONFIG,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_CONFIG) {
            IncomingServerSettingsScreen(
                onBack = onBack,
                onNext = { navController.navigateToValidation() },
                viewModel = koinViewModel<EditIncomingServerSettingsViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_VALIDATION) {
            ServerValidationScreen(
                onBack = { navController.popBackStack() },
                onNext = onFinish,
                viewModel = koinViewModel<IncomingServerValidationViewModel> {
                    parametersOf(accountUuid)
                },
            )
        }
    }
}
