package net.thunderbird.app.common.feature

import com.fsck.k9.preferences.DrawerConfigManager
import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract

internal class NavigationDrawerConfigLoader(private val drawerConfigManager: DrawerConfigManager) :
    NavigationDrawerExternalContract.DrawerConfigLoader {
    override fun loadDrawerConfigFlow(): Flow<NavigationDrawerExternalContract.DrawerConfig> {
        return drawerConfigManager.getConfigFlow()
    }
}
