package net.thunderbird.core.preference.privacy

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

private const val TAG = "DefaultPrivacySettingsPreferenceManager"

class DefaultPrivacySettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : PrivacySettingsPreferenceManager {
    private val configState: MutableStateFlow<PrivacySettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    override fun getConfig(): PrivacySettings = configState.value
    override fun getConfigFlow(): Flow<PrivacySettings> = configState

    override fun save(config: PrivacySettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): PrivacySettings = PrivacySettings(
        isHideTimeZone = storage.getBoolean(
            key = KEY_HIDE_TIME_ZONE,
            defValue = PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE,
        ),
        isHideUserAgent = storage.getBoolean(
            key = KEY_HIDE_USER_AGENT,
            defValue = PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT,
        ),
    )

    private fun writeConfig(config: PrivacySettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_HIDE_TIME_ZONE, config.isHideTimeZone)
                storageEditor.putBoolean(KEY_HIDE_USER_AGENT, config.isHideUserAgent)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
