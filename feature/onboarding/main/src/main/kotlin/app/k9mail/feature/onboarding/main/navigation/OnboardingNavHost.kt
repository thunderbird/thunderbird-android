package app.k9mail.feature.onboarding.main.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.k9mail.feature.account.common.ui.AppTitleTopHeader
import app.k9mail.feature.account.setup.navigation.AccountSetupNavHost
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.onboarding.welcome.ui.WelcomeScreen
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.feature.thundermail.ui.screen.AddThundermailAccountScreenProvider
import org.koin.compose.koinInject

private const val NESTED_NAVIGATION_ROUTE_WELCOME = "welcome"
private const val NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP = "account_setup"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT = "settings_import"
private const val NESTED_NAVIGATION_ROUTE_SETTINGS_IMPORT_QR_CODE = "settings_import_qr_code"
private const val NESTED_NAVIGATION_ROUTE_PERMISSIONS = "permissions"
private const val NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT = "add_thundermail_account"
private const val NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER = "qr_code_thundermail"
private const val NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION = "skipToIncomingValidation"

private fun NavController.navigateToAddThundermailAccount() {
    navigate(NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT)
}

private fun NavController.navigateToQrCodeScanner() {
    navigate(NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER)
}

private fun NavController.navigateToAccountSetup(skipToIncomingValidation: Boolean = false) {
    navigate(
        NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP +
            "?$NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION=$skipToIncomingValidation",
    )
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
    addThundermailAccountScreenProvider: AddThundermailAccountScreenProvider = koinInject(),
) {
    val navController = rememberNavController()
    var accountUuid by rememberSaveable { mutableStateOf<String?>(null) }

    fun onImportSuccess() {
        navController.navigateToPermissions()
    }
    SharedTransitionLayout {
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
                    animatedVisibilityScope = this,
                    appNameProvider = koinInject(),
                )
            }

            composable(route = NESTED_NAVIGATION_ROUTE_ADD_THUNDERMAIL_ACCOUNT) {
                val provider = koinInject<BrandNameProvider>()
                addThundermailAccountScreenProvider.Content(
                    header = {
                        AppTitleTopHeader(
                            title = provider.brandName,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = MainTheme.spacings.quadruple),
                        )
                    },
                    onScanQrCodeClick = { navController.navigateToQrCodeScanner() },
                    onSetupAnotherAccountClick = { navController.navigateToAccountSetup() },
                    onOAuthSuccess = { navController.navigateToAccountSetup(skipToIncomingValidation = true) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            composable(route = NESTED_NAVIGATION_ROUTE_QR_CODE_SCANNER) {
                SettingsImportScreen(
                    action = SettingsImportAction.ScanQrCode,
                    onImportSuccess = ::onImportSuccess,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = "$NESTED_NAVIGATION_ROUTE_ACCOUNT_SETUP?$NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION=" +
                    "{$NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION}",
                arguments = listOf(
                    navArgument(NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { backStackEntry ->
                val skipToIncomingValidation = backStackEntry.arguments
                    ?.getBoolean(NESTED_NAVIGATION_ARG_SKIP_TO_INCOMING_VALIDATION)
                    ?: false
                AccountSetupNavHost(
                    onBack = { navController.popBackStack() },
                    onFinish = { route: AccountSetupRoute ->
                        when (route) {
                            is AccountSetupRoute.AccountSetup -> {
                                navController.navigateToPermissions()
                            }
                        }
                    },
                    skipToIncomingValidation = skipToIncomingValidation,
                    animatedVisibilityScope = this,
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
                    animatedVisibilityScope = this,
                )
            }
        }
    }
}
