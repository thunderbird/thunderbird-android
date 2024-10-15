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
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager
import app.k9mail.feature.onboarding.permissions.domain.PermissionsDomainContract.UseCase.HasRuntimePermissions
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.onboarding.welcome.ui.WelcomeScreen
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import org.koin.compose.koinInject

private const val NESTED_NAVIGATION_ROUTE_WELCOME = "welcome"
private const val NESTED_NAVIGATION_ROUTE_MIGRATION = "migration"
private const val NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP = "account_setup"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT = "settings_import"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT_QR_CODE = "settings_import_qr_code"
private const val NESTED_NAVIGATION_ROUTE_PERMISSIONS = "permissions"

private fun NavController.navigateToMigration() {
    navigate(NESTED_NAVIGATION_ROUTE_MIGRATION)
}

private fun NavController.navigateToAccountSetup() {
    navigate(NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP)
}

private fun NavController.navigateToSettingsImport() {
    navigate(NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT)
}

private fun NavController.navigateToSettingsImportQrCode() {
    navigate(NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT_QR_CODE)
}

private fun NavController.navigateToPermissions() {
    navigate(NESTED_NAVIGATION_ROUTE_PERMISSIONS) {
        popUpTo(NESTED_NAVIGATION_ROUTE_WELCOME) {
            inclusive = true
        }
    }
}

@Suppress("LongMethod")
@Composable
fun OnboardingNavHost(
    onFinish: (String?) -> Unit,
    hasRuntimePermissions: HasRuntimePermissions = koinInject(),
    onboardingMigrationManager: OnboardingMigrationManager = koinInject(),
) {
    val navController = rememberNavController()
    var accountUuid by rememberSaveable { mutableStateOf<String?>(null) }

    fun onImportSuccess() {
        if (hasRuntimePermissions()) {
            navController.navigateToPermissions()
        } else {
            onFinish(null)
        }
    }

    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_ROUTE_WELCOME,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_WELCOME) {
            WelcomeScreen(
                onStartClick = {
                    if (onboardingMigrationManager.isFeatureIncluded()) {
                        navController.navigateToMigration()
                    } else {
                        navController.navigateToAccountSetup()
                    }
                },
                onImportClick = { navController.navigateToSettingsImport() },
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_MIGRATION) {
            onboardingMigrationManager.OnboardingMigrationScreen(
                onQrCodeScanClick = { navController.navigateToSettingsImportQrCode() },
                onAddAccountClick = { navController.navigateToAccountSetup() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP) {
            AccountSetupNavHost(
                onBack = { navController.popBackStack() },
                onFinish = { createdAccountUuid: String ->
                    accountUuid = createdAccountUuid
                    if (hasRuntimePermissions()) {
                        navController.navigateToPermissions()
                    } else {
                        onFinish(createdAccountUuid)
                    }
                },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT) {
            SettingsImportScreen(
                action = SettingsImportAction.Overview,
                onImportSuccess = ::onImportSuccess,
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT_QR_CODE) {
            SettingsImportScreen(
                action = SettingsImportAction.ScanQrCode,
                onImportSuccess = ::onImportSuccess,
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
