package net.thunderbird.core.preference.display

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val TAG = "DefaultDisplaySettingsPreferenceManager"

class DefaultDisplaySettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val coreSettingsPreferenceManager: DisplayCoreSettingsPreferenceManager,
    private val inboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
    private val visualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
) : DisplaySettingsPreferenceManager {
    private val configState: MutableStateFlow<DisplaySettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DisplaySettings = configState.value
    override fun getConfigFlow(): Flow<DisplaySettings> = configState

    override fun save(config: DisplaySettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        coreSettingsPreferenceManager.save(config.coreSettings)
        inboxSettingsPreferenceManager.save(config.inboxSettings)
        visualSettingsPreferenceManager.save(config.visualSettings)
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplaySettings = DisplaySettings(
        coreSettings = coreSettingsPreferenceManager.getConfig(),
        inboxSettings = inboxSettingsPreferenceManager.getConfig(),
        visualSettings = visualSettingsPreferenceManager.getConfig(),
        showRecentChanges = storage.getBoolean(
            KEY_SHOW_RECENT_CHANGES,
            DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
        ),
        shouldShowSetupArchiveFolderDialog = storage.getBoolean(
            KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
            DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
        ),

    )

    private fun writeConfig(config: DisplaySettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(
                    KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
                    config.shouldShowSetupArchiveFolderDialog,
                )
                storageEditor.putBoolean(KEY_SHOW_RECENT_CHANGES, config.showRecentChanges)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
