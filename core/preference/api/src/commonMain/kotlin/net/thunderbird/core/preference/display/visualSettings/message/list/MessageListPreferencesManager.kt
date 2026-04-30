package net.thunderbird.core.preference.display.visualSettings.message.list

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayMessageListSettingKey(val value: String) {

    ColorizeMissingContactPicture("colorizeMissingContactPictures"),
    ChangeRegisteredNameColor("changeRegisteredNameColor"),
    UseBackgroundAsUnreadIndicator("isUseBackgroundAsUnreadIndicator"),
    ShowCorrespondentNames("showCorrespondentNames"),
    ShowContactName("showContactName"),
    ShowContactPicture("showContactPicture"),
    MessageListPreviewLines("messageListPreviewLines"),
    MessageListDensity("messageListDensity"),
    RegisteredNameColor("registeredNameColor"),
    MessageListDateTimeFormat("messageListDateTimeFormat"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface MessageListPreferencesManager : PreferenceManager<DisplayMessageListSettings>
