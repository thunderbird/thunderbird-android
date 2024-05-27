package app.k9mail.feature.onboarding.main.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.setup.navigation.AccountSetupNavHost
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.onboarding.welcome.ui.WelcomeScreen
import app.k9mail.feature.settings.import.ui.SettingsImportScreen

private const val NESTED_NAVIGATION_ROUTE_WELCOME = "welcome"
private const val NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP = "account_setup"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT = "settings_import"
private const val NESTED_NAVIGATION_ROUTE_PERMISSIONS = "permissions"

private fun NavController.navigateToAccountSetup() {
    navigate(NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP)
}

private fun NavController.navigateToSettingsImport() {
    navigate(NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT)
}

private fun NavController.navigateToPermissions() {
    navigate(NESTED_NAVIGATION_ROUTE_PERMISSIONS) {
        popUpTo(NESTED_NAVIGATION_ROUTE_WELCOME) {
            inclusive = true
        }
    }
}

@Composable
fun OnboardingNavHost(
    onFinish: (String?) -> Unit,
) {
    val navController = rememberNavController()
    var accountUuid by rememberSaveable { mutableStateOf<String?>(null) }

    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_ROUTE_WELCOME,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_WELCOME) {
            WelcomeScreen(
                onStartClick = { navController.navigateToAccountSetup() },
                onImportClick = { navController.navigateToSettingsImport() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP) {
            AccountSetupNavHost(
                onBack = { navController.popBackStack() },
                onFinish = { createdAccountUuid: String ->
                    accountUuid = createdAccountUuid
                    navController.navigateToPermissions()
                },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT) {
            SettingsImportScreen(
                onImportSuccess = {
                    navController.navigateToPermissions()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_PERMISSIONS) {
            PermissionsScreen(
                onNext = { onFinish(accountUuid) },
            )
        }
    }
}
