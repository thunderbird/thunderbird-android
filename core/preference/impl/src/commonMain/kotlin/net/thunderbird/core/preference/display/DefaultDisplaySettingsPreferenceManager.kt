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
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.SubTheme
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultDisplaySettingsPreferenceManager"

class DefaultDisplaySettingsPreferenceManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) : DisplaySettingsPreferenceManager {
    private val configState: MutableStateFlow<DisplaySettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()

    override fun getConfig(): DisplaySettings = configState.value
    override fun getConfigFlow(): Flow<DisplaySettings> = configState

    override fun save(config: DisplaySettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplaySettings = DisplaySettings(
        fixedMessageViewTheme = storage.getBoolean(
            KEY_FIXED_MESSAGE_VIEW_THEME,
            DISPLAY_SETTINGS_FIXED_MESSAGE_VIEW_THEME,
        ),
        isShowUnifiedInbox = storage.getBoolean(KEY_SHOW_UNIFIED_INBOX, DISPLAY_SETTINGS_IS_SHOW_UNIFIED_INBOX),
        showRecentChanges = storage.getBoolean(KEY_SHOW_RECENT_CHANGES, DISPLAY_SETTINGS_SHOW_RECENT_CHANGES),
        appTheme = storage.getEnumOrDefault(KEY_THEME, AppTheme.FOLLOW_SYSTEM),
        messageViewTheme = storage.getEnumOrDefault(KEY_MESSAGE_VIEW_THEME, SubTheme.USE_GLOBAL),
        messageComposeTheme = storage.getEnumOrDefault(KEY_MESSAGE_COMPOSE_THEME, SubTheme.USE_GLOBAL),
        shouldShowSetupArchiveFolderDialog = storage.getBoolean(
            KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
            DISPLAY_SETTINGS_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
        ),
        isColorizeMissingContactPictures = storage.getBoolean(
            KEY_COLORIZE_MISSING_CONTACT_PICTURE,
            DISPLAY_SETTINGS_IS_COLORIZE_MISSING_CONTACT_PICTURE,
        ),
        isChangeContactNameColor = storage.getBoolean(
            KEY_CHANGE_REGISTERED_NAME_COLOR,
            DISPLAY_SETTINGS_IS_CHANGE_CONTACT_NAME_COLOR,
        ),
        isUseBackgroundAsUnreadIndicator = storage.getBoolean(
            KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR,
            DISPLAY_SETTINGS_IS_USE_BACKGROUND_AS_INDICATOR,
        ),
        isShowComposeButtonOnMessageList = storage.getBoolean(
            KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
            DISPLAY_SETTINGS_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
        ),
        isThreadedViewEnabled = storage.getBoolean(KEY_THREAD_VIEW_ENABLED, DISPLAY_SETTINGS_IS_THREAD_VIEW_ENABLED),
        isUseMessageViewFixedWidthFont = storage.getBoolean(
            KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT,
            DISPLAY_SETTINGS_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT,
        ),
        isAutoFitWidth = storage.getBoolean(KEY_AUTO_FIT_WIDTH, DISPLAY_SETTINGS_IS_AUTO_FIT_WIDTH),
        isShowStarredCount = storage.getBoolean(KEY_SHOW_STAR_COUNT, DISPLAY_SETTINGS_IS_SHOW_STAR_COUNT),
        isShowMessageListStars = storage.getBoolean(
            KEY_SHOW_MESSAGE_LIST_STARS,
            DISPLAY_SETTINGS_IS_SHOW_MESSAGE_LIST_STAR,
        ),
        isShowAnimations = storage.getBoolean(KEY_ANIMATION, DISPLAY_SETTINGS_IS_SHOW_ANIMATION),
        isShowCorrespondentNames = storage.getBoolean(
            KEY_SHOW_CORRESPONDENT_NAMES,
            DISPLAY_SETTINGS_IS_SHOW_CORRESPONDENT_NAMES,
        ),
        isMessageListSenderAboveSubject = storage.getBoolean(
            KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
            DISPLAY_SETTINGS_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
        ),
        isShowContactName = storage.getBoolean(KEY_SHOW_CONTACT_NAME, DISPLAY_SETTINGS_IS_SHOW_CONTACT_NAME),
        isShowContactPicture = storage.getBoolean(KEY_SHOW_CONTACT_PICTURE, DISPLAY_SETTINGS_IS_SHOW_CONTACT_PICTURE),
    )

    private fun writeConfig(config: DisplaySettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putEnum(KEY_THEME, config.appTheme)
                storageEditor.putEnum(KEY_MESSAGE_VIEW_THEME, config.messageViewTheme)
                storageEditor.putEnum(KEY_MESSAGE_COMPOSE_THEME, config.messageComposeTheme)
                storageEditor.putBoolean(KEY_FIXED_MESSAGE_VIEW_THEME, config.fixedMessageViewTheme)
                storageEditor.putBoolean(KEY_SHOW_UNIFIED_INBOX, config.isShowUnifiedInbox)
                storageEditor.putBoolean(KEY_CHANGE_REGISTERED_NAME_COLOR, config.isChangeContactNameColor)
                storageEditor.putBoolean(KEY_COLORIZE_MISSING_CONTACT_PICTURE, config.isColorizeMissingContactPictures)
                storageEditor.putBoolean(
                    KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
                    config.shouldShowSetupArchiveFolderDialog,
                )
                storageEditor.putBoolean(KEY_SHOW_STAR_COUNT, config.isShowStarredCount)
                storageEditor.putBoolean(KEY_SHOW_CONTACT_NAME, config.isShowContactName)
                storageEditor.putBoolean(KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT, config.isMessageListSenderAboveSubject)
                storageEditor.putBoolean(KEY_SHOW_CORRESPONDENT_NAMES, config.isShowCorrespondentNames)
                storageEditor.putBoolean(KEY_SHOW_MESSAGE_LIST_STARS, config.isShowMessageListStars)
                storageEditor.putBoolean(KEY_SHOW_RECENT_CHANGES, config.showRecentChanges)
                storageEditor.putBoolean(KEY_ANIMATION, config.isShowAnimations)
                storageEditor.putBoolean(KEY_SHOW_CONTACT_PICTURE, config.isShowContactPicture)
                storageEditor.putBoolean(
                    KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR,
                    config.isUseBackgroundAsUnreadIndicator,
                )
                storageEditor.putBoolean(
                    KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
                    config.isShowComposeButtonOnMessageList,
                )
                storageEditor.putBoolean(KEY_THREAD_VIEW_ENABLED, config.isThreadedViewEnabled)
                storageEditor.putBoolean(KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT, config.isUseMessageViewFixedWidthFont)
                storageEditor.putBoolean(KEY_AUTO_FIT_WIDTH, config.isAutoFitWidth)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    companion object {
        private const val KEY_FIXED_MESSAGE_VIEW_THEME = "fixedMessageViewTheme"
        private const val KEY_SHOW_UNIFIED_INBOX = "showUnifiedInbox"
        private const val KEY_SHOW_STAR_COUNT = "showStarredCount"
        private const val KEY_SHOW_CONTACT_NAME = "showContactName"
        private const val KEY_SHOW_CORRESPONDENT_NAMES = "showCorrespondentNames"
        private const val KEY_SHOW_MESSAGE_LIST_STARS = "messageListStars"
        private const val KEY_SHOW_RECENT_CHANGES = "showRecentChanges"
        private const val KEY_THEME = "theme"
        private const val KEY_ANIMATION = "animations"
        private const val KEY_MESSAGE_VIEW_THEME = "messageViewTheme"
        private const val KEY_MESSAGE_COMPOSE_THEME = "messageComposeTheme"
        private const val KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = "shouldShowSetupArchiveFolderDialog"
        private const val KEY_CHANGE_REGISTERED_NAME_COLOR = "changeRegisteredNameColor"
        private const val KEY_COLORIZE_MISSING_CONTACT_PICTURE = "colorizeMissingContactPictures"
        private const val KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR = "isUseBackgroundAsUnreadIndicator"
        private const val KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST = "showComposeButtonOnMessageList"
        private const val KEY_THREAD_VIEW_ENABLED = "isThreadedViewEnabled"
        private const val KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT = "messageViewFixedWidthFont"
        private const val KEY_AUTO_FIT_WIDTH = "autofitWidth"
        private const val KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT = "messageListSenderAboveSubject"
        private const val KEY_SHOW_CONTACT_PICTURE = "showContactPicture"
    }
}
