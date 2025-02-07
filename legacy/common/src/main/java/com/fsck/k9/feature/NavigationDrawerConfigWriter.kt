package com.fsck.k9.feature

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract
import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import com.fsck.k9.preferences.DrawerConfigManager

class NavigationDrawerConfigWriter(
    private val drawerConfigManager: DrawerConfigManager,
) : NavigationDrawerExternalContract.DrawerConfigWriter {
    override fun writeDrawerConfig(drawerConfig: DrawerConfig) {
        drawerConfigManager.save(drawerConfig)
    }
}
