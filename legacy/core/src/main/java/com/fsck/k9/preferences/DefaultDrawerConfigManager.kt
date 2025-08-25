package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.update
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawerExternalContract.DrawerConfig

internal class DefaultDrawerConfigManager(
    private val preferences: Preferences,
    coroutineScope: CoroutineScope,
    private val displayInboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
) : DrawerConfigManager {
    private val showAccountSelector = MutableStateFlow(K9.isShowAccountSelector)
    private val drawerConfig: StateFlow<DrawerConfig> = showAccountSelector
        .combine(displayInboxSettingsPreferenceManager.getConfigFlow()) { showAccSelector, displayInboxSettings ->
            DrawerConfig(
                showAccountSelector = showAccSelector,
                showStarredCount = displayInboxSettings.isShowStarredCount,
                showUnifiedFolders = displayInboxSettings.isShowUnifiedInbox,
            )
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = DrawerConfig(
                showAccountSelector = false,
                showStarredCount = false,
                showUnifiedFolders = false,
            ),
        )

    override fun save(config: DrawerConfig) {
        displayInboxSettingsPreferenceManager.update {
            it.copy(
                isShowStarredCount = config.showStarredCount,
                isShowUnifiedInbox = config.showUnifiedFolders,
            )
        }

        val editor = preferences.createStorageEditor()
        K9.save(editor)
        editor.putBoolean("showAccountSelector", config.showAccountSelector)
        editor.commit()
        showAccountSelector.update { config.showAccountSelector }
    }

    @Synchronized
    override fun getConfig(): DrawerConfig {
        return drawerConfig.value
    }

    override fun getConfigFlow(): Flow<DrawerConfig> {
        return drawerConfig
    }
}
