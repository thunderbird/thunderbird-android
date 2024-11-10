package com.fsck.k9.feature

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfigLoader
import com.fsck.k9.K9

class NavigationDrawerConfigLoader : DrawerConfigLoader {
    override fun loadDrawerConfig(): DrawerConfig {
        return DrawerConfig(
            showUnifiedFolders = K9.isShowUnifiedInbox,
            showStarredCount = K9.isShowStarredCount,
            showUnReadCount = K9.isShowUnReadCount,
        )
    }
}
