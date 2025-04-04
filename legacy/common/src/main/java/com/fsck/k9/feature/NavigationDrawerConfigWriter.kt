package com.fsck.k9.feature

import com.fsck.k9.preferences.DrawerConfigManager
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

class NavigationDrawerConfigWriter(
    private val drawerConfigManager: DrawerConfigManager,
) : NavigationDrawerExternalContract.DrawerConfigWriter {
    override fun writeDrawerConfig(drawerConfig: DrawerConfig) {
        drawerConfigManager.save(drawerConfig)
    }
}
