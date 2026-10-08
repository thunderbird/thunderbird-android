package com.fsck.k9.preferences

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.update
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

internal class DefaultDrawerConfigManager(
    coroutineScope: CoroutineScope,
    private val displayInboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
    private val displayVisualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
) : DrawerConfigManager {
    private val drawerConfig: StateFlow<DrawerConfig> = flow {
        emit(
            DrawerConfig(
                showStarredCount = displayInboxSettingsPreferenceManager.getConfig().isShowStarredCount,
                showUnifiedFolders = displayInboxSettingsPreferenceManager.getConfig().isShowUnifiedInbox,
                expandAllFolder = displayVisualSettingsPreferenceManager.getConfig().drawerExpandAllFolder,
            ),
        )
    }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DrawerConfig(
                showStarredCount = false,
                showUnifiedFolders = false,
                expandAllFolder = false,
            ),
        )

    override fun save(config: DrawerConfig) {
        displayInboxSettingsPreferenceManager.update {
            it.copy(
                isShowStarredCount = config.showStarredCount,
                isShowUnifiedInbox = config.showUnifiedFolders,
            )
        }
        displayVisualSettingsPreferenceManager.update {
            it.copy(drawerExpandAllFolder = config.expandAllFolder)
        }
    }

    @Synchronized
    override fun getConfig(): DrawerConfig {
        return drawerConfig.value
    }

    override fun getConfigFlow(): Flow<DrawerConfig> {
        return drawerConfig
    }
}
