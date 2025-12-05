package net.thunderbird.core.preference.display.visualSettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultDisplayVisualSettingsPreferenceManager"

class DefaultDisplayVisualSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    preferenceChangeBroker: PreferenceChangeBroker,
    private val messageListPreferences: MessageListPreferencesManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DisplayVisualSettingsPreferenceManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }
    private val internalConfigState = MutableStateFlow(value = loadConfig())
    private val configState: StateFlow<DisplayVisualSettings> = combine(
        internalConfigState,
        messageListPreferences.getConfigFlow(),
    ) { config, messageListConfig ->
        config.copy(messageListSettings = messageListConfig)
    }.stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = internalConfigState.value)
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    override fun save(config: DisplayVisualSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        internalConfigState.update { config }
    }

    private fun loadConfig(): DisplayVisualSettings = DisplayVisualSettings(
        isUseMessageViewFixedWidthFont = storage.getBoolean(
            KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT,
            DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT,
        ),
        isAutoFitWidth = storage.getBoolean(
            KEY_AUTO_FIT_WIDTH,
            DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH,
        ),
        isShowAnimations = storage.getBoolean(
            KEY_ANIMATION,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION,
        ),
        bodyContentType = storage.getEnumOrDefault(
            KEY_MESSAGE_VIEW_BODY_CONTENT_TYPE,
            DISPLAY_SETTINGS_DEFAULT_BODY_CONTENT_TYPE,
        ),
        drawerExpandAllFolder = storage.getBoolean(
            KEY_DRAWER_EXPAND_ALL_FOLDER,
            DISPLAY_SETTINGS_DEFAULT_DRAWER_EXPAND_ALL_FOLDER,
        ),
    )

    private fun writeConfig(config: DisplayVisualSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(KEY_ANIMATION, config.isShowAnimations)
                storageEditor.putBoolean(
                    KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT,
                    config.isUseMessageViewFixedWidthFont,
                )
                storageEditor.putBoolean(KEY_AUTO_FIT_WIDTH, config.isAutoFitWidth)
                storageEditor.putEnum(KEY_MESSAGE_VIEW_BODY_CONTENT_TYPE, config.bodyContentType)
                storageEditor.putBoolean(KEY_DRAWER_EXPAND_ALL_FOLDER, config.drawerExpandAllFolder)
                messageListPreferences.save(config.messageListSettings)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    override fun getConfig() = configState.value

    override fun getConfigFlow(): Flow<DisplayVisualSettings> = configState

    override fun receive() {
        internalConfigState.update { loadConfig() }
    }
}
