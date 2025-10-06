package net.thunderbird.core.preference.display

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
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val TAG = "DefaultDisplaySettingsPreferenceManager"

class DefaultDisplaySettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    private val coreSettingsPreferenceManager: DisplayCoreSettingsPreferenceManager,
) : DisplaySettingsPreferenceManager {
    private val configState: MutableStateFlow<DisplaySettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DisplaySettings = configState.value
    override fun getConfigFlow(): Flow<DisplaySettings> = configState

    override fun save(config: DisplaySettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        coreSettingsPreferenceManager.save(config.coreSettings)
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplaySettings = DisplaySettings(
        coreSettings = coreSettingsPreferenceManager.getConfig(),
        showRecentChanges = storage.getBoolean(
            KEY_SHOW_RECENT_CHANGES,
            DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
        ),
        shouldShowSetupArchiveFolderDialog = storage.getBoolean(
            KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
            DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
        ),
        isColorizeMissingContactPictures = storage.getBoolean(
            KEY_COLORIZE_MISSING_CONTACT_PICTURE,
            DISPLAY_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE,
        ),
        isChangeContactNameColor = storage.getBoolean(
            KEY_CHANGE_REGISTERED_NAME_COLOR,
            DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR,
        ),
        isUseBackgroundAsUnreadIndicator = storage.getBoolean(
            KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR,
            DISPLAY_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR,
        ),
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
        isShowCorrespondentNames = storage.getBoolean(
            KEY_SHOW_CORRESPONDENT_NAMES,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES,
        ),
        isShowContactName = storage.getBoolean(
            KEY_SHOW_CONTACT_NAME,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME,
        ),
        isShowContactPicture = storage.getBoolean(
            KEY_SHOW_CONTACT_PICTURE,
            DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE,
        ),
    )

    private fun writeConfig(config: DisplaySettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putBoolean(
                    KEY_CHANGE_REGISTERED_NAME_COLOR,
                    config.isChangeContactNameColor,
                )
                storageEditor.putBoolean(
                    KEY_COLORIZE_MISSING_CONTACT_PICTURE,
                    config.isColorizeMissingContactPictures,
                )
                storageEditor.putBoolean(
                    KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
                    config.shouldShowSetupArchiveFolderDialog,
                )
                storageEditor.putBoolean(KEY_SHOW_CONTACT_NAME, config.isShowContactName)
                storageEditor.putBoolean(
                    KEY_SHOW_CORRESPONDENT_NAMES,
                    config.isShowCorrespondentNames,
                )
                storageEditor.putBoolean(KEY_SHOW_RECENT_CHANGES, config.showRecentChanges)
                storageEditor.putBoolean(KEY_ANIMATION, config.isShowAnimations)
                storageEditor.putBoolean(
                    KEY_SHOW_CONTACT_PICTURE,
                    config.isShowContactPicture,
                )
                storageEditor.putBoolean(
                    KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR,
                    config.isUseBackgroundAsUnreadIndicator,
                )
                storageEditor.putBoolean(
                    KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT,
                    config.isUseMessageViewFixedWidthFont,
                )
                storageEditor.putBoolean(KEY_AUTO_FIT_WIDTH, config.isAutoFitWidth)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }
}
