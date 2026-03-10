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
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.config.DebugLogConfigurator
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.PreferenceChangePublisher
import net.thunderbird.core.preference.debugging.DebuggingSettingsPreferenceManager
import net.thunderbird.core.preference.display.DisplaySettingsPreferenceManager
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
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
@Suppress("LongParameterList")
internal class DefaultGeneralSettingsManager(
    private val preferences: Preferences,
    private val coroutineScope: CoroutineScope,
    private val changePublisher: PreferenceChangePublisher,
    private val privacySettingsPreferenceManager: PrivacySettingsPreferenceManager,
    private val notificationPreferenceManager: NotificationPreferenceManager,
    private val displaySettingsSettingsPreferenceManager: DisplaySettingsPreferenceManager,
    private val displayCoreSettingsPreferenceManager: DisplayCoreSettingsPreferenceManager,
    private val displayInboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
    private val displayVisualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
    private val messageListPreferencesManager: MessageListPreferencesManager,
    private val displayMiscSettingsPreferenceManager: DisplayMiscSettingsPreferenceManager,
    private val networkSettingsPreferenceManager: NetworkSettingsPreferenceManager,
    private val debuggingSettingsPreferenceManager: DebuggingSettingsPreferenceManager,
    private val interactionSettingsPreferenceManager: InteractionSettingsPreferenceManager,
    private val debugLogConfigurator: DebugLogConfigurator,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val platformConfigProvider: PlatformConfigProvider,
    private val logger: Logger,
) : GeneralSettingsManager {
    val mutex = Mutex()
    private val generalSettings = privacySettingsPreferenceManager.getConfigFlow()
        .map { privacy ->
            GeneralSettings(privacy = privacy, platformConfigProvider = platformConfigProvider)
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
        .combine(displayCoreSettingsPreferenceManager.getConfigFlow()) { generalSettings, coreSettings ->
            generalSettings.copy(
                display = generalSettings.display.copy(coreSettings = coreSettings),
            )
        }
        .combine(displayInboxSettingsPreferenceManager.getConfigFlow()) { generalSettings, inboxSettings ->
            generalSettings.copy(
                display = generalSettings.display.copy(inboxSettings = inboxSettings),
            )
        }
        .combine(displayVisualSettingsPreferenceManager.getConfigFlow()) { generalSettings, visualSettings ->
            generalSettings.copy(
                display = generalSettings.display.copy(visualSettings = visualSettings),
            )
        }
        .combine(messageListPreferencesManager.getConfigFlow()) { generalSettings, messageListSettings ->
            logger.debug { "messageListSettings: $messageListSettings" }
            generalSettings.copy(
                display = generalSettings.display.copy(
                    visualSettings = generalSettings.display.visualSettings.copy(
                        messageListSettings = messageListSettings,
                    ),
                ),
            )
        }
        .combine(displayMiscSettingsPreferenceManager.getConfigFlow()) { generalSettings, miscSettings ->
            generalSettings.copy(
                display = generalSettings.display.copy(miscSettings = miscSettings),
            )
        }
        .combine(networkSettingsPreferenceManager.getConfigFlow()) { generalSettings, networkSettings ->
            generalSettings.copy(
                network = networkSettings,
            )
        }
        .combine(debuggingSettingsPreferenceManager.getConfigFlow()) { generalSettings, debuggingSettings ->
            generalSettings.copy(
                debugging = debuggingSettings,
            ).also {
                debugLogConfigurator.updateLoggingStatus(debuggingSettings.isDebugLoggingEnabled)
                debugLogConfigurator.updateSyncLogging(debuggingSettings.isSyncLoggingEnabled)
            }
        }
        .combine(interactionSettingsPreferenceManager.getConfigFlow()) { generalSettings, interactionSettings ->
            generalSettings.copy(interaction = interactionSettings)
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = GeneralSettings(platformConfigProvider = platformConfigProvider),
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
                displayCoreSettingsPreferenceManager.save(config.display.coreSettings)
                displayInboxSettingsPreferenceManager.save(config.display.inboxSettings)
                displayVisualSettingsPreferenceManager.save(config.display.visualSettings)
                messageListPreferencesManager.save(config.display.visualSettings.messageListSettings)
                displayMiscSettingsPreferenceManager.save(config.display.miscSettings)
                networkSettingsPreferenceManager.save(config.network)
                debuggingSettingsPreferenceManager.save(config.debugging)
                interactionSettingsPreferenceManager.save(config.interaction)
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
