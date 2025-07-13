@file:Suppress("DEPRECATION")

package com.fsck.k9.preferences

import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.QuietTimeChecker
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
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
// TODO(#9432): Split GeneralSettings and GeneralSettingsManager in smaller classes/interfaces
@Suppress("TooManyFunctions")
internal class RealGeneralSettingsManager(
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

    // TODO(#9432): Should be removed when K9 settings is completely migrated
    // This fallback is required until we finalize the split of the GeneralSettings class and Manager.
    // The GeneralSettings must be composed by other smaller Managers flows.
    private val k9GeneralSettingsFallback = MutableStateFlow(value = loadGeneralSettings())

    private val generalSettings = MutableStateFlow(value = loadGeneralSettings())
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
        // TODO(#9232): Should be removed when K9 settings is completely migrated
        k9GeneralSettingsFallback.update { loadGeneralSettings() }
    }

    @Deprecated(message = "This only exists for collaboration with the K9 class")
    fun saveSettingsAsync() {
        coroutineScope.launch(backgroundDispatcher) {
            val settings = updateGeneralSettingsWithStateFromK9()
            save(config = settings)
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
    @Deprecated("This only exists for collaboration with the K9 class and should be removed after #9232")
    private fun updateGeneralSettingsWithStateFromK9(): GeneralSettings {
        return getSettings().also { generalSettings ->
            k9GeneralSettingsFallback.update { generalSettings }
        }
    }

    @Synchronized
    private fun saveSettings() {
        val editor = preferences.createStorageEditor()
        K9.save(editor)
        editor.commit()
        changePublisher.publish()
    }

    private fun loadGeneralSettings(): GeneralSettings {
        val settings = GeneralSettings()
        return settings
    }

    private fun getIsQuietTime(isQuietTimeEnabled: Boolean, quietTimeStarts: String, quietTimeEnds: String): Boolean {
        if (!isQuietTimeEnabled) return false

        val clock = DI.get<Clock>()
        val quietTimeChecker = QuietTimeChecker(
            clock = clock,
            quietTimeStart = quietTimeStarts,
            quietTimeEnd = quietTimeEnds,
        )
    private fun getIsQuietTime(): Boolean {
        val (isQuietTimeEnabled, quietTimeStarts, quietTimeEnds) = generalSettings?.let { settings ->
            Triple(
                settings.isQuietTimeEnabled,
                settings.quietTimeStarts,
                settings.quietTimeEnds,
            )
        } ?: run {
            Triple(
                storage.getBoolean(KEY_QUIET_TIME_ENABLED, false),
                storage.getStringOrDefault(KEY_QUIET_TIME_STARTS, "21:00"),
                storage.getStringOrDefault(KEY_QUIET_TIME_ENDS, "7:00"),
            )
        }

        if (isQuietTimeEnabled) {
            return false
        }

        @OptIn(ExperimentalTime::class)
        val quietTimeChecker = QuietTimeChecker(
            clock = DI.get<Clock>(),
            quietTimeStart = quietTimeStarts,
            quietTimeEnd = quietTimeEnds,
        )
        return quietTimeChecker.isQuietTime
    }
}
