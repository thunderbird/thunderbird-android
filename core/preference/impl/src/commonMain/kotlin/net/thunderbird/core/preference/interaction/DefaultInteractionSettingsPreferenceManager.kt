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
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
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
    preferenceChangeBroker: PreferenceChangeBroker,
) : InteractionSettingsPreferenceManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }
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
            InteractionSettingKey.UseVolumeKeysForNavigation.value,
            INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
        ),
        messageViewPostRemoveNavigation = storage.getStringOrDefault(
            InteractionSettingKey.MessageViewPostDeleteAction.value,
            INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
        ),
        messageViewPostMarkAsUnreadNavigation = storage.getEnumOrDefault(
            InteractionSettingKey.MessageViewPostMarkAsRead.value,
            INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_MARK_AS_UNREAD_NAVIGATION,
        ),
        swipeActions = SwipeActions(
            leftAction = storage.getEnumOrDefault(
                key = InteractionSettingKey.SwipeActionLeft.value,
                default = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION.leftAction,
            ),
            rightAction = storage.getEnumOrDefault(
                key = InteractionSettingKey.SwipeActionRight.value,
                default = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION.rightAction,
            ),
        ),
        isConfirmDelete = storage.getBoolean(
            InteractionSettingKey.ConfirmDelete.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE,
        ),
        isConfirmDeleteStarred = storage.getBoolean(
            InteractionSettingKey.ConfirmDeleteStarred.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED,
        ),
        isConfirmDeleteFromNotification = storage.getBoolean(
            InteractionSettingKey.ConfirmDeleteFromNotification.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION,
        ),
        isConfirmSpam = storage.getBoolean(
            InteractionSettingKey.ConfirmSpam.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM,
        ),
        isConfirmDiscardMessage = storage.getBoolean(
            InteractionSettingKey.ConfirmDiscardMessage.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE,
        ),
        isConfirmMarkAllRead = storage.getBoolean(
            InteractionSettingKey.ConfirmMarkAllRead.value,
            INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ,
        ),
    )

    private fun writeConfig(config: InteractionSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(
                    InteractionSettingKey.UseVolumeKeysForNavigation.value,
                    config.useVolumeKeysForNavigation,
                )
                storageEditor.putString(
                    InteractionSettingKey.MessageViewPostDeleteAction.value,
                    config.messageViewPostRemoveNavigation,
                )
                storageEditor.putEnum(
                    InteractionSettingKey.MessageViewPostMarkAsRead.value,
                    config.messageViewPostMarkAsUnreadNavigation,
                )
                storageEditor.putEnum(InteractionSettingKey.SwipeActionLeft.value, config.swipeActions.leftAction)
                storageEditor.putEnum(InteractionSettingKey.SwipeActionRight.value, config.swipeActions.rightAction)
                storageEditor.putBoolean(InteractionSettingKey.ConfirmDelete.value, config.isConfirmDelete)
                storageEditor.putBoolean(
                    InteractionSettingKey.ConfirmDiscardMessage.value,
                    config.isConfirmDiscardMessage,
                )
                storageEditor.putBoolean(
                    InteractionSettingKey.ConfirmDeleteStarred.value,
                    config.isConfirmDeleteStarred,
                )
                storageEditor.putBoolean(InteractionSettingKey.ConfirmSpam.value, config.isConfirmSpam)
                storageEditor.putBoolean(
                    InteractionSettingKey.ConfirmDeleteFromNotification.value,
                    config.isConfirmDeleteFromNotification,
                )
                storageEditor.putBoolean(InteractionSettingKey.ConfirmMarkAllRead.value, config.isConfirmMarkAllRead)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.INTERACTION) {
            configState.update { loadConfig() }
        }
    }
}
