package net.thunderbird.core.preference.network

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
import net.thunderbird.core.preference.BackgroundOps
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultNetworkSettingsPreferenceManager"
class DefaultNetworkSettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : NetworkSettingsPreferenceManager {
    private val configState: MutableStateFlow<NetworkSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): NetworkSettings = configState.value
    override fun getConfigFlow(): Flow<NetworkSettings> = configState

    override fun save(config: NetworkSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): NetworkSettings = NetworkSettings(
        backgroundOps = storage.getEnumOrDefault(KEY_BG_OPS, BackgroundOps.ALWAYS),
    )

    private fun writeConfig(config: NetworkSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putEnum(KEY_BG_OPS, config.backgroundOps)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    companion object {
        private const val KEY_BG_OPS = "backgroundOperations"
    }
}
