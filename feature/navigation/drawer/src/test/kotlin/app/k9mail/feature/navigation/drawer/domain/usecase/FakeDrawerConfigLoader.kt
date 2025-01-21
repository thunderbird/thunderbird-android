package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader

internal class FakeDrawerConfigLoader(
    var drawerConfig: DrawerConfig = DrawerConfig(showUnifiedFolders = false, showStarredCount = false),
) : DrawerConfigLoader {
    override fun loadDrawerConfig(): DrawerConfig {
        return drawerConfig
    }
}
