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
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultInteractionSettingsPreferenceManager"

class DefaultInteractionSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : InteractionSettingsPreferenceManager {
    private val configState: MutableStateFlow<InteractionSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    private val storage: Storage
        get() = storagePersister.loadValues()

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
        swipeActions = SwipeActions(
            leftAction = storage.getEnumOrDefault(
                key = KEY_SWIPE_ACTION_LEFT,
                default = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION.leftAction,
            ),
            rightAction = storage.getEnumOrDefault(
                key = KEY_SWIPE_ACTION_RIGHT,
                default = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION.rightAction,
            ),
        ),
        isConfirmDelete = storage.getBoolean(KEY_CONFIRM_DELETE, INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE),
        isConfirmDeleteStarred = storage.getBoolean(
            KEY_CONFIRM_DISCARD_MESSAGE,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED,
        ),
        isConfirmDeleteFromNotification = storage.getBoolean(
            KEY_CONFIRM_DELETE_STARRED,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION,
        ),
        isConfirmSpam = storage.getBoolean(KEY_CONFIRM_SPAM, INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM),
        isConfirmDiscardMessage = storage.getBoolean(
            KEY_CONFIRM_DELETE_FROM_NOTIFICATION,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE,
        ),
        isConfirmMarkAllRead = storage.getBoolean(
            KEY_CONFIRM_MARK_ALL_READ,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ,
        ),
    )

    private fun writeConfig(config: InteractionSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_USE_VOLUME_KEYS_FOR_NAVIGATION, config.useVolumeKeysForNavigation)
                storageEditor.putString(KEY_MESSAGE_VIEW_POST_DELETE_ACTION, config.messageViewPostRemoveNavigation)
                storageEditor.putEnum(KEY_SWIPE_ACTION_LEFT, config.swipeActions.leftAction)
                storageEditor.putEnum(KEY_SWIPE_ACTION_RIGHT, config.swipeActions.rightAction)
                storageEditor.putBoolean(KEY_CONFIRM_DELETE, config.isConfirmDelete)
                storageEditor.putBoolean(KEY_CONFIRM_DISCARD_MESSAGE, config.isConfirmDiscardMessage)
                storageEditor.putBoolean(KEY_CONFIRM_DELETE_STARRED, config.isConfirmDeleteStarred)
                storageEditor.putBoolean(KEY_CONFIRM_SPAM, config.isConfirmSpam)
                storageEditor.putBoolean(KEY_CONFIRM_DELETE_FROM_NOTIFICATION, config.isConfirmDeleteFromNotification)
                storageEditor.putBoolean(KEY_CONFIRM_MARK_ALL_READ, config.isConfirmMarkAllRead)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
