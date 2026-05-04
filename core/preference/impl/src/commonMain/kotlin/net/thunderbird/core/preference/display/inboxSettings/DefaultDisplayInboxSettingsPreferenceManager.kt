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
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister

private const val TAG = "DefaultDisplayInboxSettingsPreferenceManager"

class DefaultDisplayInboxSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    preferenceChangeBroker: PreferenceChangeBroker,
) : DisplayInboxSettingsPreferenceManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }
    private val configState: MutableStateFlow<DisplayInboxSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    override fun getConfig(): DisplayInboxSettings = configState.value

    override fun getConfigFlow(): Flow<DisplayInboxSettings> = configState

    override fun save(config: DisplayInboxSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplayInboxSettings = DisplayInboxSettings(
        isShowUnifiedInbox = storage.getBoolean(
            DisplayInboxSettingKey.ShowUnifiedInbox.value,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX,
        ),
        isShowComposeButtonOnMessageList = storage.getBoolean(
            DisplayInboxSettingKey.ShowComposeButtonOnMessageList.value,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
        ),
        isThreadedViewEnabled = storage.getBoolean(
            DisplayInboxSettingKey.ThreadViewEnabled.value,
            DISPLAY_SETTINGS_DEFAULT_IS_THREAD_VIEW_ENABLED,
        ),
        isShowStarredCount = storage.getBoolean(
            DisplayInboxSettingKey.ShowStarCount.value,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT,
        ),
        isShowMessageListStars = storage.getBoolean(
            DisplayInboxSettingKey.ShowMessageListStars.value,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR,
        ),
        isMessageListSenderAboveSubject = storage.getBoolean(
            DisplayInboxSettingKey.MessageListSenderAboveSubject.value,
            DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
        ),
    )

    private fun writeConfig(config: DisplayInboxSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(
                    DisplayInboxSettingKey.MessageListSenderAboveSubject.value,
                    config.isMessageListSenderAboveSubject,
                )
                storageEditor.putBoolean(
                    DisplayInboxSettingKey.ShowMessageListStars.value,
                    config.isShowMessageListStars,
                )
                storageEditor.putBoolean(
                    DisplayInboxSettingKey.ShowComposeButtonOnMessageList.value,
                    config.isShowComposeButtonOnMessageList,
                )
                storageEditor.putBoolean(
                    DisplayInboxSettingKey.ThreadViewEnabled.value,
                    config.isThreadedViewEnabled,
                )
                storageEditor.putBoolean(DisplayInboxSettingKey.ShowUnifiedInbox.value, config.isShowUnifiedInbox)
                storageEditor.putBoolean(DisplayInboxSettingKey.ShowStarCount.value, config.isShowStarredCount)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.DISPLAY_INBOX) {
            configState.update { loadConfig() }
        }
    }
}
