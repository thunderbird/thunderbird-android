package net.thunderbird.feature.debug.settings.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import app.k9mail.core.ui.compose.navigation.deepLinkComposable
import net.thunderbird.feature.debug.settings.SecretDebugSettingsScreen

internal class DefaultSecretDebugSettingsNavigation : SecretDebugSettingsNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (SecretDebugSettingsRoute) -> Unit,
    ) {
        with(navGraphBuilder) {
            deepLinkComposable<SecretDebugSettingsRoute>(
                basePath = SecretDebugSettingsRoute().basePath,
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<SecretDebugSettingsRoute>()
                SecretDebugSettingsScreen(
                    starterTab = route.tab,
                    onNavigateBack = onBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
