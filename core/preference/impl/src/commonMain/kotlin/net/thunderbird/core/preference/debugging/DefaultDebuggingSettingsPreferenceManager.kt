package net.thunderbird.core.preference.debugging

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
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogLevelManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val TAG = "DefaultDebuggingSettingsPreferenceManager"

class DefaultDebuggingSettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val logLevelManager: LogLevelManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DebuggingSettingsPreferenceManager {
    private val configState: MutableStateFlow<DebuggingSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DebuggingSettings = configState.value
    override fun getConfigFlow(): Flow<DebuggingSettings> = configState

    override fun save(config: DebuggingSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config.also(::updateDebugLogLevel) }
    }

    private fun loadConfig(): DebuggingSettings = DebuggingSettings(
        isDebugLoggingEnabled = storage.getBoolean(
            KEY_ENABLE_DEBUG_LOGGING,
            DEBUGGING_SETTINGS_DEFAULT_IS_DEBUGGING_LOGGING_ENABLED,
        ),
        isSyncLoggingEnabled = storage.getBoolean(
            KEY_ENABLE_SYNC_DEBUG_LOGGING,
            DEBUGGING_SETTINGS_DEFAULT_IS_SYNC_LOGGING_ENABLED,
        ),
    ).also(::updateDebugLogLevel)

    private fun writeConfig(config: DebuggingSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_ENABLE_DEBUG_LOGGING, config.isDebugLoggingEnabled)
                storageEditor.putBoolean(KEY_ENABLE_SYNC_DEBUG_LOGGING, config.isSyncLoggingEnabled)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    private fun updateDebugLogLevel(config: DebuggingSettings) {
        if (config.isDebugLoggingEnabled) {
            logLevelManager.override(LogLevel.VERBOSE)
        } else {
            logLevelManager.restoreDefault()
        }
    }
}
