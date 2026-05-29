package net.thunderbird.feature.thundermail.internal.common.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.feature.account.setup.navigation.AccountSetupNavHost
import app.k9mail.feature.account.setup.navigation.AccountSetupRoute
import app.k9mail.feature.onboarding.permissions.ui.PermissionsScreen
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import net.thunderbird.core.ui.navigation.deepLinkComposable
import net.thunderbird.feature.thundermail.internal.common.ui.ThundermailOAuthRedirectScreen
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute.Companion.ACCOUNT_ID_ROUTE_PARAM

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
                ThundermailOAuthRedirectScreen(onFinish = onFinish, onBack = onBack)
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
