package com.fsck.k9.feature

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader
import app.k9mail.feature.navigation.drawer.domain.entity.DrawerConfig
import com.fsck.k9.K9

class NavigationDrawerConfigLoader : DrawerConfigLoader {
    override fun loadDrawerConfig(): DrawerConfig {
        return DrawerConfig(
            showUnifiedInbox = K9.isShowUnifiedInbox,
            showStarredCount = K9.isShowStarredCount,
        )
    }
}
