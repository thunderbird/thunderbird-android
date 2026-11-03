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
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StorageProvider
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultInteractionSettingsPreferenceManager"
private val KEYPAD_DIGIT_ACTION_KEYS = listOf(
    InteractionSettingKey.KeypadDigit0Action,
    InteractionSettingKey.KeypadDigit1Action,
    InteractionSettingKey.KeypadDigit2Action,
    InteractionSettingKey.KeypadDigit3Action,
    InteractionSettingKey.KeypadDigit4Action,
    InteractionSettingKey.KeypadDigit5Action,
    InteractionSettingKey.KeypadDigit6Action,
    InteractionSettingKey.KeypadDigit7Action,
    InteractionSettingKey.KeypadDigit8Action,
    InteractionSettingKey.KeypadDigit9Action,
)

class DefaultInteractionSettingsPreferenceManager(
    private val logger: Logger,
    private val storageProvider: StorageProvider,
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
        get() = storageProvider.storage

    override fun getConfig(): InteractionSettings = configState.value
    override fun getConfigFlow(): Flow<InteractionSettings> = configState

    override fun save(config: InteractionSettings) {
        val normalizedConfig = config.copy(
            keypadMessageActions = config.keypadMessageActions
                .filter { (digit, action) -> digit in 0..9 && action != KeypadMessageAction.NONE }
                .toMap(),
        )
        logger.debug(TAG) { "save() called with: config = $normalizedConfig" }
        writeConfig(normalizedConfig)
        configState.update { normalizedConfig }
    }

    private fun loadConfig(): InteractionSettings = InteractionSettings(
        useVolumeKeysForNavigation = storage.getBoolean(
            InteractionSettingKey.UseVolumeKeysForNavigation.value,
            INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
        ),
        isKeypadNavigationEnabled = storage.getBoolean(
            InteractionSettingKey.KeypadNavigationEnabled.value,
            INTERACTION_SETTINGS_DEFAULT_KEYPAD_NAVIGATION_ENABLED,
        ),
        keypadMessageActions = loadKeypadMessageActions(),
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

    private fun loadKeypadMessageActions(): Map<Int, KeypadMessageAction> = buildMap {
        KEYPAD_DIGIT_ACTION_KEYS.forEachIndexed { digit, key ->
            val action = storage.getEnumOrDefault(key.value, KeypadMessageAction.NONE)
            if (action != KeypadMessageAction.NONE) put(digit, action)
        }
    }

    private fun writeConfig(config: InteractionSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                val storageEditor = storageProvider.createStorageEditor()
                storageEditor.putBoolean(
                    InteractionSettingKey.UseVolumeKeysForNavigation.value,
                    config.useVolumeKeysForNavigation,
                )
                storageEditor.putBoolean(
                    InteractionSettingKey.KeypadNavigationEnabled.value,
                    config.isKeypadNavigationEnabled,
                )
                KEYPAD_DIGIT_ACTION_KEYS.forEachIndexed { digit, key ->
                    storageEditor.putEnum(key.value, config.keypadMessageActions[digit] ?: KeypadMessageAction.NONE)
                }
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
