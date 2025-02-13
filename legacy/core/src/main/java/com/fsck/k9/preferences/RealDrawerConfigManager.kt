package com.fsck.k9.preferences

import app.k9mail.feature.navigation.drawer.NavigationDrawerExternalContract.DrawerConfig
import app.k9mail.legacy.preferences.SettingsChangeBroker
import app.k9mail.legacy.preferences.SettingsChangeSubscriber
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal class RealDrawerConfigManager(
    private val preferences: Preferences,
    private val coroutineScope: CoroutineScope,
    private val changeBroker: SettingsChangeBroker,
) : DrawerConfigManager {
    private val drawerConfigFlow = MutableSharedFlow<DrawerConfig>(replay = 1)

    init {
        coroutineScope.launch {
            asSettingsFlow().collect { config ->
                drawerConfigFlow.emit(config)
            }
        }
    }

    override fun save(config: DrawerConfig) {
        saveDrawerConfig(config)
        updateDrawerConfigFlow(config)
    }

    private fun loadDrawerConfig(): DrawerConfig {
        return DrawerConfig(
            showAccountSelector = K9.isShowAccountSelector,
            showStarredCount = K9.isShowStarredCount,
            showUnifiedFolders = K9.isShowUnifiedInbox,
        )
    }

    private fun updateDrawerConfigFlow(config: DrawerConfig) {
        coroutineScope.launch {
            drawerConfigFlow.emit(config)
        }
    }

    @Synchronized
    override fun getConfig(): DrawerConfig {
        return loadDrawerConfig().also {
            updateDrawerConfigFlow(it)
        }
    }

    override fun getConfigFlow(): Flow<DrawerConfig> {
        return drawerConfigFlow.distinctUntilChanged()
    }

    private fun asSettingsFlow(): Flow<DrawerConfig> {
        return callbackFlow {
            send(loadDrawerConfig())

            val subscriber = SettingsChangeSubscriber {
                drawerConfigFlow.tryEmit(loadDrawerConfig())
            }

            changeBroker.subscribe(subscriber)

            awaitClose {
                changeBroker.unsubscribe(subscriber)
            }
        }
    }

    @Synchronized
    private fun saveDrawerConfig(config: DrawerConfig) {
        val editor = preferences.createStorageEditor()
        K9.save(editor)
        writeDrawerConfig(editor, config)
        editor.commit()
    }

    private fun writeDrawerConfig(editor: StorageEditor, config: DrawerConfig) {
        editor.putBoolean("showAccountSelector", config.showAccountSelector)
        editor.putBoolean("showUnifiedInbox", config.showUnifiedFolders)
        editor.putBoolean("showStarredCount", config.showStarredCount)
    }
}
