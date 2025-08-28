package net.thunderbird.feature.debug.settings.navigation

import androidx.navigation.NavGraphBuilder

object NoOpSecretDebugSettingsNavigation : SecretDebugSettingsNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (SecretDebugSettingsRoute) -> Unit,
    ) = Unit
}
