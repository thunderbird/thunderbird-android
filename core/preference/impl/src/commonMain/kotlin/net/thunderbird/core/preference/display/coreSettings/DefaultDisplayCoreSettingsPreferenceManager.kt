package net.thunderbird.core.preference.display.coreSettings

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
import net.thunderbird.core.preference.display.KEY_THEME
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultDisplayCoreSettingsPreferenceManager"

class DefaultDisplayCoreSettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DisplayCoreSettingsPreferenceManager {

    private val configState: MutableStateFlow<DisplayCoreSetting> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DisplayCoreSetting = configState.value

    override fun getConfigFlow(): Flow<DisplayCoreSetting> = configState

    override fun save(config: DisplayCoreSetting) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplayCoreSetting = DisplayCoreSetting(
        fixedMessageViewTheme = storage.getBoolean(
            KEY_FIXED_MESSAGE_VIEW_THEME,
            DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME,
        ),
        appTheme = storage.getEnumOrDefault(KEY_THEME, DISPLAY_SETTINGS_DEFAULT_APP_THEME),
        messageViewTheme = storage.getEnumOrDefault(
            KEY_MESSAGE_VIEW_THEME,
            DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME,
        ),
        messageComposeTheme = storage.getEnumOrDefault(
            KEY_MESSAGE_COMPOSE_THEME,
            DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME,
        ),
        appLanguage = storage.getStringOrDefault(
            KEY_APP_LANGUAGE,
            DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE,
        ),
        splitViewMode = storage.getEnumOrDefault(
            KEY_SPLIT_VIEW_MODE,
            DISPLAY_SETTINGS_DEFAULT_SPLIT_VIEW_MODE,
        ),
    )

    private fun writeConfig(config: DisplayCoreSetting) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putEnum(KEY_THEME, config.appTheme)
                storageEditor.putEnum(KEY_MESSAGE_VIEW_THEME, config.messageViewTheme)
                storageEditor.putEnum(
                    KEY_MESSAGE_COMPOSE_THEME,
                    config.messageComposeTheme,
                )
                storageEditor.putBoolean(
                    KEY_FIXED_MESSAGE_VIEW_THEME,
                    config.fixedMessageViewTheme,
                )
                storageEditor.putString(KEY_APP_LANGUAGE, config.appLanguage)
                storageEditor.putEnum(KEY_SPLIT_VIEW_MODE, config.splitViewMode)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
