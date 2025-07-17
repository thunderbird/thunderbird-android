package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.PreferenceChangePublisher
import net.thunderbird.core.preference.display.DisplaySettingsPreferenceManager
import net.thunderbird.core.preference.network.NetworkSettingsPreferenceManager
import net.thunderbird.core.preference.notification.NotificationPreferenceManager
import net.thunderbird.core.preference.privacy.PrivacySettingsPreferenceManager
import net.thunderbird.core.preference.storage.Storage

/**
 * Retrieve and modify general settings.
 *
 * Currently general settings are split between [K9] and [GeneralSettings]. The goal is to move everything over to
 * [GeneralSettings] and get rid of [K9].
 *
 * The [GeneralSettings] instance managed by this class is updated with state from [K9] when [K9.saveSettingsAsync] is
 * called.
 */
internal class DefaultGeneralSettingsManager(
    private val preferences: Preferences,
    private val coroutineScope: CoroutineScope,
    private val changePublisher: PreferenceChangePublisher,
    private val privacySettingsPreferenceManager: PrivacySettingsPreferenceManager,
    private val notificationPreferenceManager: NotificationPreferenceManager,
    private val displaySettingsSettingsPreferenceManager: DisplaySettingsPreferenceManager,
    private val networkSettingsPreferenceManager: NetworkSettingsPreferenceManager,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GeneralSettingsManager {
    val mutex = Mutex()
    private val generalSettings = privacySettingsPreferenceManager.getConfigFlow()
        .map { privacy ->
            GeneralSettings(privacy = privacy)
        }
        .combine(privacySettingsPreferenceManager.getConfigFlow()) { generalSettings, privacySettings ->
            generalSettings.copy(
                privacy = privacySettings,
            )
        }
        .combine(notificationPreferenceManager.getConfigFlow()) { generalSettings, notificationSettings ->
            generalSettings.copy(
                notification = notificationSettings,
            )
        }
        .combine(displaySettingsSettingsPreferenceManager.getConfigFlow()) { generalSettings, displaySettings ->
            generalSettings.copy(
                display = displaySettings,
            )
        }
        .combine(networkSettingsPreferenceManager.getConfigFlow()) { generalSettings, networkSettings ->
            generalSettings.copy(
                network = networkSettings,
            )
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = GeneralSettings(),
        )

    @Deprecated("This only exists for collaboration with the K9 class")
    val storage: Storage
        get() = preferences.storage

    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfig() instead",
        replaceWith = ReplaceWith(expression = "getConfig()"),
    )
    @Synchronized
    override fun getSettings(): GeneralSettings = generalSettings.value

    @Deprecated(
        message = "Use PreferenceManager<GeneralSettings>.getConfigFlow() instead",
        replaceWith = ReplaceWith(expression = "getConfigFlow()"),
    )
    override fun getSettingsFlow(): Flow<GeneralSettings> = generalSettings

    override fun getConfig(): GeneralSettings = generalSettings.value

    override fun getConfigFlow(): Flow<GeneralSettings> = generalSettings

    @Synchronized
    fun loadSettings() {
        K9.loadPrefs(preferences.storage)
    }

    @Deprecated(message = "This only exists for collaboration with the K9 class")
    fun saveSettingsAsync() {
        coroutineScope.launch(backgroundDispatcher) {
            save(config = getConfig())
        }
    }

    override fun save(config: GeneralSettings) {
        coroutineScope.launch(backgroundDispatcher) {
            mutex.withLock {
                saveSettings()
                privacySettingsPreferenceManager.save(config.privacy)
                notificationPreferenceManager.save(config.notification)
                displaySettingsSettingsPreferenceManager.save(config.display)
                networkSettingsPreferenceManager.save(config.network)
            }
        }
    }

    @Synchronized
    private fun saveSettings() {
        val editor = preferences.createStorageEditor()
        K9.save(editor)
        editor.commit()
        changePublisher.publish()
    }
}
