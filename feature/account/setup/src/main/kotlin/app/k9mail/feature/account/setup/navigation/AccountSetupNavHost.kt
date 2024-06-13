package app.k9mail.feature.account.setup.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.k9mail.feature.account.common.domain.entity.IncomingProtocolType
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsScreen
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsScreen
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsViewModel
import app.k9mail.feature.account.server.validation.ui.IncomingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.OutgoingServerValidationViewModel
import app.k9mail.feature.account.server.validation.ui.ServerValidationScreen
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryScreen
import app.k9mail.feature.account.setup.ui.autodiscovery.AccountAutoDiscoveryViewModel
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountScreen
import app.k9mail.feature.account.setup.ui.createaccount.CreateAccountViewModel
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsScreen
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsViewModel
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsScreen
import app.k9mail.feature.account.setup.ui.options.sync.SyncOptionsViewModel
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersScreen
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private const val NESTED_NAVIGATION_AUTO_CONFIG = "autoconfig"
private const val NESTED_NAVIGATION_INCOMING_SERVER_CONFIG = "incoming-server/config"
private const val NESTED_NAVIGATION_INCOMING_SERVER_VALIDATION = "incoming-server/validation"
private const val NESTED_NAVIGATION_OUTGOING_SERVER_CONFIG = "outgoing-server/config"
private const val NESTED_NAVIGATION_OUTGOING_SERVER_VALIDATION = "outgoing-server/validation"
private const val NESTED_NAVIGATION_SPECIAL_FOLDERS = "special-folders"
private const val NESTED_NAVIGATION_DISPLAY_OPTIONS = "display-options"
private const val NESTED_NAVIGATION_SYNC_OPTIONS = "sync-options"
private const val NESTED_NAVIGATION_CREATE_ACCOUNT = "create-account"

@Suppress("LongMethod")
@Composable
fun AccountSetupNavHost(
    onBack: () -> Unit,
    onFinish: (String) -> Unit,
) {
    val navController = rememberNavController()
    var isAutomaticConfig by rememberSaveable { mutableStateOf(false) }
    var hasSpecialFolders by rememberSaveable { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_AUTO_CONFIG,
    ) {
        composable(route = NESTED_NAVIGATION_AUTO_CONFIG) {
            AccountAutoDiscoveryScreen(
                onNext = { result ->
                    isAutomaticConfig = result.isAutomaticConfig
                    if (isAutomaticConfig) {
                        hasSpecialFolders = checkSpecialFoldersSupport(result.incomingProtocolType)
                        navController.navigate(NESTED_NAVIGATION_INCOMING_SERVER_VALIDATION)
                    } else {
                        navController.navigate(NESTED_NAVIGATION_INCOMING_SERVER_CONFIG)
                    }
                },
                onBack = onBack,
                viewModel = koinViewModel<AccountAutoDiscoveryViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_INCOMING_SERVER_CONFIG) {
            IncomingServerSettingsScreen(
                onNext = { state ->
                    hasSpecialFolders = checkSpecialFoldersSupport(state.protocolType)
                    navController.navigate(NESTED_NAVIGATION_INCOMING_SERVER_VALIDATION)
                },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<IncomingServerSettingsViewModel>(),
            )
        }

        composable(route = NESTED_NAVIGATION_INCOMING_SERVER_VALIDATION) {
            ServerValidationScreen(
                onNext = {
                    if (isAutomaticConfig) {
                        navController.navigate(NESTED_NAVIGATION_OUTGOING_SERVER_VALIDATION) {
                            popUpTo(NESTED_NAVIGATION_AUTO_CONFIG)
                        }
                    } else {
                        navController.navigate(NESTED_NAVIGATION_OUTGOING_SERVER_CONFIG) {
                            popUpTo(NESTED_NAVIGATION_INCOMING_SERVER_CONFIG)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<IncomingServerValidationViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_OUTGOING_SERVER_CONFIG) {
            OutgoingServerSettingsScreen(
                onNext = { navController.navigate(NESTED_NAVIGATION_OUTGOING_SERVER_VALIDATION) },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<OutgoingServerSettingsViewModel>(),
            )
        }

        composable(route = NESTED_NAVIGATION_OUTGOING_SERVER_VALIDATION) {
            ServerValidationScreen(
                onNext = {
                    navController.navigate(
                        if (hasSpecialFolders) {
                            NESTED_NAVIGATION_SPECIAL_FOLDERS
                        } else {
                            NESTED_NAVIGATION_DISPLAY_OPTIONS
                        },
                    ) {
                        if (isAutomaticConfig) {
                            popUpTo(NESTED_NAVIGATION_AUTO_CONFIG)
                        } else {
                            popUpTo(NESTED_NAVIGATION_OUTGOING_SERVER_CONFIG)
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<OutgoingServerValidationViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_SPECIAL_FOLDERS) {
            SpecialFoldersScreen(
                onNext = { isManualSetup ->
                    navController.navigate(NESTED_NAVIGATION_DISPLAY_OPTIONS) {
                        if (isManualSetup) {
                            popUpTo(NESTED_NAVIGATION_SPECIAL_FOLDERS)
                        } else {
                            if (isAutomaticConfig) {
                                popUpTo(NESTED_NAVIGATION_AUTO_CONFIG)
                            } else {
                                popUpTo(NESTED_NAVIGATION_OUTGOING_SERVER_CONFIG)
                            }
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<SpecialFoldersViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_DISPLAY_OPTIONS) {
            DisplayOptionsScreen(
                onNext = { navController.navigate(NESTED_NAVIGATION_SYNC_OPTIONS) },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<DisplayOptionsViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_SYNC_OPTIONS) {
            SyncOptionsScreen(
                onNext = { navController.navigate(NESTED_NAVIGATION_CREATE_ACCOUNT) },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<SyncOptionsViewModel>(),
                appNameProvider = koinInject(),
            )
        }

        composable(route = NESTED_NAVIGATION_CREATE_ACCOUNT) {
            CreateAccountScreen(
                onNext = { accountUuid -> onFinish(accountUuid.value) },
                onBack = { navController.popBackStack() },
                viewModel = koinViewModel<CreateAccountViewModel>(),
                appNameProvider = koinInject(),
            )
        }
    }
}

internal fun checkSpecialFoldersSupport(protocolType: IncomingProtocolType?): Boolean {
    return protocolType == IncomingProtocolType.IMAP
}
