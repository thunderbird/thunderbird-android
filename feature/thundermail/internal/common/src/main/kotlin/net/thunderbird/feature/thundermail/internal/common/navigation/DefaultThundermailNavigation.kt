package net.thunderbird.feature.thundermail.internal.common.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.designsystem.atom.CircularProgressIndicator
import app.k9mail.core.ui.compose.designsystem.atom.text.TextBodyLarge
import app.k9mail.feature.account.setup.navigation.AccountSetupNavHost
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import net.thunderbird.core.ui.compose.theme2.MainTheme
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.core.ui.navigation.deepLinkComposable
import net.thunderbird.feature.thundermail.internal.common.R
import net.thunderbird.feature.thundermail.internal.common.ui.ThundermailContract
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute.Companion.ACCOUNT_ID_ROUTE_PARAM
import org.koin.androidx.compose.koinViewModel

class DefaultThundermailNavigation : ThundermailNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (ThundermailRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<ThundermailRoute.SignInWithThundermail>(
                basePath = ThundermailRoute.SIGN_IN_WITH_THUNDERMAIL_ROUTE,
            ) {
                ThundermailOAuthRedirectScreen(onFinish = onFinish)
            }

            deepLinkComposable<ThundermailRoute.ScanQrCode>(
                basePath = ThundermailRoute.SCAN_QR_CODE_ROUTE,
            ) {
                SettingsImportScreen(
                    action = SettingsImportAction.ScanQrCode,
                    onImportSuccess = { onFinish(ThundermailRoute.AccountSetup(accountId = null)) },
                    onBack = onBack,
                )
            }

            deepLinkComposable<ThundermailRoute.IncomingSettings>(
                basePath = ThundermailRoute.INCOMING_SETTINGS_ROUTE,
            ) {
                AccountSetupNavHost(
                    onBack = onBack,
                    onFinish = { route: AccountSetupRoute ->
                        when (route) {
                            is AccountSetupRoute.AccountSetup -> onFinish(
                                ThundermailRoute.Permissions(
                                    requireNotNull(route.accountId) {
                                        "Account ID must not be null when navigating to AccountSetupRoute.AccountSetup"
                                    },
                                ),
                            )

                            AccountSetupRoute.ThundermailScanQrCode -> onFinish(ThundermailRoute.ScanQrCode)
                            AccountSetupRoute.ThundermailSignIn -> onFinish(ThundermailRoute.SignInWithThundermail)
                        }
                    },
                    skipToIncomingValidation = true,
                )
            }

            deepLinkComposable<ThundermailRoute.Permissions>(
                basePath = ThundermailRoute.PERMISSIONS_ROUTE,
            ) {
                val accountId = requireNotNull(it.arguments?.getString(ACCOUNT_ID_ROUTE_PARAM)) {
                    "Account ID must not be null when navigating to Permissions"
                }
                PermissionsScreen(
                    onNext = { onFinish(ThundermailRoute.OnboardComplete(accountId)) },
                )
            }
        }
    }
}

@Composable
private fun ThundermailOAuthRedirectScreen(
    viewModel: ThundermailContract.ViewModel = koinViewModel<ThundermailContract.ViewModel>(),
    onFinish: (ThundermailRoute) -> Unit,
) {
    val oAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.event(ThundermailContract.Event.OnOAuthResult(it.resultCode, it.data))
    }

    val (state, dispatch) = viewModel.observe { effect ->
        when (effect) {
            is ThundermailContract.Effect.LaunchOAuth -> oAuthLauncher.launch(effect.intent)
            is ThundermailContract.Effect.NavigateToIncomingServerSettings ->
                onFinish(ThundermailRoute.IncomingSettings)
        }
    }

    var launchedOAuth by remember { mutableStateOf(false) }

    LaunchedEffect(state.value.initialized) {
        if (state.value.initialized && !launchedOAuth) {
            dispatch(ThundermailContract.Event.SignInClicked)
            launchedOAuth = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding(),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.spacedBy(MainTheme.spacings.double),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(MainTheme.sizes.medium),
            )
            TextBodyLarge(stringResource(R.string.thundermail_redirecting))
        }
    }
}
