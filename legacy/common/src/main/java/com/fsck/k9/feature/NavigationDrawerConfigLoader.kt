package com.fsck.k9.feature

import com.fsck.k9.preferences.DrawerConfigManager
import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfigLoader

class NavigationDrawerConfigLoader(private val drawerConfigManager: DrawerConfigManager) : DrawerConfigLoader {
    override fun loadDrawerConfigFlow(): Flow<DrawerConfig> {
        return drawerConfigManager.getConfigFlow()
    }
}
