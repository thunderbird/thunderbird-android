package net.thunderbird.core.preference.display.visualSettings.message.list

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor

private const val TAG = "DefaultMessageListPreferencesManager"

class DefaultMessageListPreferencesManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
) : MessageListPreferencesManager {
    private val preferences = MutableStateFlow(value = loadPreferences())
    override fun save(config: DisplayMessageListSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        write(config)
        preferences.update { config }
    }

    private fun loadPreferences(): DisplayMessageListSettings = DisplayMessageListSettings(
        isColorizeMissingContactPictures = storage.getBoolean(
            KEY_COLORIZE_MISSING_CONTACT_PICTURE,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE,
        ),
        isChangeContactNameColor = storage.getBoolean(
            KEY_CHANGE_REGISTERED_NAME_COLOR,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR,
        ),
        isUseBackgroundAsUnreadIndicator = storage.getBoolean(
            KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR,
        ),
        isShowCorrespondentNames = storage.getBoolean(
            KEY_SHOW_CORRESPONDENT_NAMES,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES,
        ),
        isShowContactName = storage.getBoolean(
            KEY_SHOW_CONTACT_NAME,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME,
        ),
        isShowContactPicture = storage.getBoolean(
            KEY_SHOW_CONTACT_PICTURE,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE,
        ),
        previewLines = storage.getInt(
            KEY_MESSAGE_LIST_VIEW_PREVIEW_LINES,
            MESSAGE_LIST_SETTINGS_DEFAULT_PREVIEW_LINES,
        ),
    )

    private fun write(preferences: DisplayMessageListSettings) {
        storageEditor.putBoolean(KEY_CHANGE_REGISTERED_NAME_COLOR, preferences.isChangeContactNameColor)
        storageEditor.putBoolean(KEY_COLORIZE_MISSING_CONTACT_PICTURE, preferences.isColorizeMissingContactPictures)
        storageEditor.putBoolean(KEY_SHOW_CONTACT_PICTURE, preferences.isShowContactPicture)
        storageEditor.putBoolean(KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR, preferences.isUseBackgroundAsUnreadIndicator)
        storageEditor.putBoolean(KEY_SHOW_CONTACT_NAME, preferences.isShowContactName)
        storageEditor.putBoolean(KEY_SHOW_CORRESPONDENT_NAMES, preferences.isShowCorrespondentNames)
        storageEditor.putInt(KEY_MESSAGE_LIST_VIEW_PREVIEW_LINES, preferences.previewLines)
    }

    override fun getConfig(): DisplayMessageListSettings = preferences.value
    override fun getConfigFlow(): Flow<DisplayMessageListSettings> = preferences
}
