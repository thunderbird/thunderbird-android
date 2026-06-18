package net.thunderbird.core.preference.display.visualSettings.message.list

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultMessageListPreferencesManager"

class DefaultMessageListPreferencesManager(
    private val logger: Logger,
    private val storage: Storage,
    private val storageEditor: StorageEditor,
    preferenceChangeBroker: PreferenceChangeBroker,
) : MessageListPreferencesManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }
    private val preferences = MutableStateFlow(value = loadPreferences())
    override fun save(config: DisplayMessageListSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        write(config)
        preferences.update { config }
    }

    private fun loadPreferences(): DisplayMessageListSettings = DisplayMessageListSettings(
        isColorizeMissingContactPictures = storage.getBoolean(
            DisplayMessageListSettingKey.ColorizeMissingContactPicture.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE,
        ),
        isChangeContactNameColor = storage.getBoolean(
            DisplayMessageListSettingKey.ChangeRegisteredNameColor.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR,
        ),
        isUseBackgroundAsUnreadIndicator = storage.getBoolean(
            DisplayMessageListSettingKey.UseBackgroundAsUnreadIndicator.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR,
        ),
        isShowCorrespondentNames = storage.getBoolean(
            DisplayMessageListSettingKey.ShowCorrespondentNames.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES,
        ),
        isShowContactName = storage.getBoolean(
            DisplayMessageListSettingKey.ShowContactName.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME,
        ),
        isShowContactPicture = storage.getBoolean(
            DisplayMessageListSettingKey.ShowContactPicture.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE,
        ),
        previewLines = storage.getInt(
            DisplayMessageListSettingKey.MessageListPreviewLines.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_PREVIEW_LINES,
        ),
        uiDensity = storage.getEnumOrDefault(
            DisplayMessageListSettingKey.MessageListDensity.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_UI_DENSITY,
        ),
        contactNameColor = storage.getInt(
            DisplayMessageListSettingKey.RegisteredNameColor.value,
            DISPLAY_SETTINGS_DEFAULT_CONTACT_NAME_COLOR,
        ),
        dateTimeFormat = storage.getEnumOrDefault(
            DisplayMessageListSettingKey.MessageListDateTimeFormat.value,
            MESSAGE_LIST_SETTINGS_DEFAULT_DATE_TIME_FORMAT,
        ),
    )

    private fun write(preferences: DisplayMessageListSettings) {
        storageEditor.putBoolean(
            DisplayMessageListSettingKey.ChangeRegisteredNameColor.value,
            preferences.isChangeContactNameColor,
        )
        storageEditor.putBoolean(
            DisplayMessageListSettingKey.ColorizeMissingContactPicture.value,
            preferences.isColorizeMissingContactPictures,
        )
        storageEditor.putBoolean(
            DisplayMessageListSettingKey.ShowContactPicture.value,
            preferences.isShowContactPicture,
        )
        storageEditor.putBoolean(
            DisplayMessageListSettingKey.UseBackgroundAsUnreadIndicator.value,
            preferences.isUseBackgroundAsUnreadIndicator,
        )
        storageEditor.putBoolean(DisplayMessageListSettingKey.ShowContactName.value, preferences.isShowContactName)
        storageEditor.putBoolean(
            DisplayMessageListSettingKey.ShowCorrespondentNames.value,
            preferences.isShowCorrespondentNames,
        )
        storageEditor.putInt(DisplayMessageListSettingKey.MessageListPreviewLines.value, preferences.previewLines)
        storageEditor.putInt(DisplayMessageListSettingKey.RegisteredNameColor.value, preferences.contactNameColor)
        storageEditor.putEnum(DisplayMessageListSettingKey.MessageListDensity.value, preferences.uiDensity)
        storageEditor.putEnum(DisplayMessageListSettingKey.MessageListDateTimeFormat.value, preferences.dateTimeFormat)
        storageEditor.commit().also { commited ->
            logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
        }
    }

    override fun getConfig(): DisplayMessageListSettings = preferences.value
    override fun getConfigFlow(): Flow<DisplayMessageListSettings> = preferences

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.DISPLAY_VISUAL_MESSAGE_LIST) {
            preferences.update { loadPreferences() }
        }
    }
}
