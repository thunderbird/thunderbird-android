package net.thunderbird.feature.debug.settings.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.feature.debug.settings.SecretDebugSettingsScreen
import net.thunderbird.feature.debug.settings.navigation.SecretDebugSettingsRoute.Notification

internal class DefaultSecretDebugSettingsNavigation : SecretDebugSettingsNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (SecretDebugSettingsRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<Notification>(Notification.basePath) {
                SecretDebugSettingsScreen(
                    onNavigateBack = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
