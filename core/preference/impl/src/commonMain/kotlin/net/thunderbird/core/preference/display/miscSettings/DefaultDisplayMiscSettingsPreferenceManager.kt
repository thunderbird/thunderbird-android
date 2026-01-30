package net.thunderbird.core.preference.display.miscSettings

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
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister

private const val TAG = "DefaultDisplayMiscSettingsPreferenceManager"

class DefaultDisplayMiscSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DisplayMiscSettingsPreferenceManager {
    private val configState: MutableStateFlow<DisplayMiscSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()
    override fun save(config: DisplayMiscSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    override fun getConfig(): DisplayMiscSettings = configState.value

    override fun getConfigFlow(): Flow<DisplayMiscSettings> = configState

    private fun loadConfig(): DisplayMiscSettings = DisplayMiscSettings(
        showRecentChanges = storage.getBoolean(
            KEY_SHOW_RECENT_CHANGES,
            DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
        ),
        shouldShowSetupArchiveFolderDialog = storage.getBoolean(
            KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
            DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
        ),
    )

    private fun writeConfig(config: DisplayMiscSettings) {
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
