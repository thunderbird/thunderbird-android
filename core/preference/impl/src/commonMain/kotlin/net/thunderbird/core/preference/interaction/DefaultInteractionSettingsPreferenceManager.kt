package net.thunderbird.core.preference.interaction

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

private const val TAG = "DefaultInteractionSettingsPreferenceManager"

class DefaultInteractionSettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : InteractionSettingsPreferenceManager {
    private val configState: MutableStateFlow<InteractionSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): InteractionSettings = configState.value
    override fun getConfigFlow(): Flow<InteractionSettings> = configState

    override fun save(config: InteractionSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): InteractionSettings = InteractionSettings(
        useVolumeKeysForNavigation = storage.getBoolean(
            KEY_USE_VOLUME_KEYS_FOR_NAVIGATION,
            INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
        ),
        messageViewPostRemoveNavigation = storage.getStringOrDefault(
            KEY_MESSAGE_VIEW_POST_DELETE_ACTION,
            INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
        ),
    )

    private fun writeConfig(config: InteractionSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_USE_VOLUME_KEYS_FOR_NAVIGATION, config.useVolumeKeysForNavigation)
                storageEditor.putString(KEY_MESSAGE_VIEW_POST_DELETE_ACTION, config.messageViewPostRemoveNavigation)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
