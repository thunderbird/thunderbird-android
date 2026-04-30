package net.thunderbird.core.preference.display.inboxSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayInboxSettingKey(val value: String) {

    MessageListSenderAboveSubject("messageListSenderAboveSubject"),
    ShowComposeButtonOnMessageList("showComposeButtonOnMessageList"),
    ShowMessageListStars("messageListStars"),
    ShowStarCount("showStarredCount"),
    ShowUnifiedInbox("showUnifiedInbox"),
    ThreadViewEnabled("isThreadedViewEnabled"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface DisplayInboxSettingsPreferenceManager : PreferenceManager<DisplayInboxSettings>
