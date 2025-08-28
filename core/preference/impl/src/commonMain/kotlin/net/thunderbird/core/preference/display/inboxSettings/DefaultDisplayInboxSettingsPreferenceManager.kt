package net.thunderbird.core.preference.display.inboxSettings

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

private const val TAG = "DefaultDisplayInboxSettingsPreferenceManager"

class DefaultDisplayInboxSettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DisplayInboxSettingsPreferenceManager {

    private val configState: MutableStateFlow<DisplayInboxSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DisplayInboxSettings = configState.value

    override fun getConfigFlow(): Flow<DisplayInboxSettings> = configState

    override fun save(config: DisplayInboxSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplayInboxSettings = DisplayInboxSettings(
        isShowUnifiedInbox = storage.getBoolean(
            KEY_SHOW_UNIFIED_INBOX,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX,
        ),
        isShowComposeButtonOnMessageList = storage.getBoolean(
            KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
        ),
        isThreadedViewEnabled = storage.getBoolean(
            KEY_THREAD_VIEW_ENABLED,
            DISPLAY_SETTINGS_DEFAULT_IS_THREAD_VIEW_ENABLED,
        ),
        isShowStarredCount = storage.getBoolean(
            KEY_SHOW_STAR_COUNT,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT,
        ),
        isShowMessageListStars = storage.getBoolean(
            KEY_SHOW_MESSAGE_LIST_STARS,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR,
        ),
        isMessageListSenderAboveSubject = storage.getBoolean(
            KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
            DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
        ),
    )

    private fun writeConfig(config: DisplayInboxSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(
                    KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
                    config.isMessageListSenderAboveSubject,
                )
                storageEditor.putBoolean(
                    KEY_SHOW_MESSAGE_LIST_STARS,
                    config.isShowMessageListStars,
                )
                storageEditor.putBoolean(
                    KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
                    config.isShowComposeButtonOnMessageList,
                )
                storageEditor.putBoolean(
                    KEY_THREAD_VIEW_ENABLED,
                    config.isThreadedViewEnabled,
                )
                storageEditor.putBoolean(KEY_SHOW_UNIFIED_INBOX, config.isShowUnifiedInbox)
                storageEditor.putBoolean(KEY_SHOW_STAR_COUNT, config.isShowStarredCount)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
