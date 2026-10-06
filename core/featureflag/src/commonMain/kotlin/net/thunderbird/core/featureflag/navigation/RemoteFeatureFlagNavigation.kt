package net.thunderbird.core.featureflag.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.featureflag.ui.RemoteFeatureFlagScreen
import net.thunderbird.core.ui.navigation.Navigation
import net.thunderbird.core.ui.navigation.deepLinkComposable

interface RemoteFeatureFlagNavigation : Navigation<RemoteFeatureFlagRoute>

internal class DefaultRemoteFeatureFlagNavigation(
    private val platformConfigProvider: PlatformConfigProvider,
) : RemoteFeatureFlagNavigation {
    override fun registerRoutes(
        navGraphBuilder: NavGraphBuilder,
        onBack: () -> Unit,
        onFinish: (RemoteFeatureFlagRoute) -> Unit,
    ) = with(navGraphBuilder) {
        deepLinkComposable<RemoteFeatureFlagRoute>(basePath = RemoteFeatureFlagRoute.basePath) {
            RemoteFeatureFlagScreen(
                isDebugBuild = platformConfigProvider.isDebug,
                onBack = onBack,
                onDebugClick = { onFinish(RemoteFeatureFlagRoute) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
