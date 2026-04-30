package net.thunderbird.feature.thundermail.internal.common.navigation

import androidx.navigation.NavGraphBuilder
import app.k9mail.feature.settings.import.ui.SettingsImportAction
import app.k9mail.feature.settings.import.ui.SettingsImportScreen
import net.thunderbird.core.ui.navigation.deepLinkComposable
import net.thunderbird.feature.thundermail.navigation.ThundermailNavigation
import net.thunderbird.feature.thundermail.navigation.ThundermailRoute

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
        }
    }
}
