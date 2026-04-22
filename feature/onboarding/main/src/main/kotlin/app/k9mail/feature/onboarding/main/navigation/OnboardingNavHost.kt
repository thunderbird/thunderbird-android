package app.k9mail.feature.onboarding.main.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.setup.navigation.AccountSetupNavHost
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.onboarding.migration.api.OnboardingMigrationManager
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.onboarding.welcome.ui.WelcomeScreen
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenProvider
import org.koin.compose.koinInject

private const val NESTED_NAVIGATION_ROUTE_WELCOME = "welcome"
private const val NESTED_NAVIGATION_ROUTE_MIGRATION = "migration"
private const val NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP = "account_setup"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT = "settings_import"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT_QR_CODE = "settings_import_qr_code"
private const val NESTED_NAVIGATION_ROUTE_PERMISSIONS = "permissions"
private const val NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT = "add_thundermail_account"
private const val NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER = "qr_code_thundermail"

private fun NavController.navigateToAddThundermailAccount() {
    navigate(NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT)
}

private fun NavController.navigateToQrCodeScanner() {
    navigate(NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER)
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
    onFinish: (OnboardingRoute) -> Unit,
    modifier: Modifier = Modifier,
    onboardingMigrationManager: OnboardingMigrationManager = koinInject(),
    addThundermailAccountScreenProvider: AddThundermailAccountScreenProvider = koinInject(),
) {
    val navController = rememberNavController()
    var accountUuid by rememberSaveable { mutableStateOf<String?>(null) }

    fun onImportSuccess() {
        navController.navigateToPermissions()
    }

    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_ROUTE_WELCOME,
        modifier = modifier,
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_WELCOME) {
            WelcomeScreen(
                onStartClick = {
                    navController.navigateToAddThundermailAccount()
                },
                onImportClick = { navController.navigateToSettingsImport() },
                appNameProvider = koinInject(),
                onboardingMigrationManager = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT) {
            val appNameProvider = koinInject<AppNameProvider>()
            addThundermailAccountScreenProvider.Content(
                header = {
                    AppTitleTopHeader(
                        title = appNameProvider.appName,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                onSignWithThundermailClick = {
                    // TODO(#10911): Navigate to OAuth
                },
                onScanQrCodeClick = { navController.navigateToQrCodeScanner() },
                onSetupAnotherAccountClick = { navController.navigateToAccountSetup() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER) {
            SettingsImportScreen(
                action = SettingsImportAction.ScanQrCode,
                onImportSuccess = ::onImportSuccess,
                onBack = { navController.popBackStack() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_MIGRATION) {
            onboardingMigrationManager.OnboardingMigrationScreen(
                onQrCodeScan = { navController.navigateToSettingsImportQrCode() },
                onAddAccount = { navController.navigateToAccountSetup() },
                onImport = { navController.navigateToSettingsImport() },
            )
        }

        composable(route = NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP) {
            AccountSetupNavHost(
                onBack = { navController.popBackStack() },
                onFinish = { route: AccountSetupRoute ->
                    when (route) {
                        is AccountSetupRoute.AccountSetup -> {
                            navController.navigateToPermissions()
                        }
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
                onNext = { onFinish(OnboardingRoute.Onboarding(accountUuid)) },
            )
        }
    }
}
