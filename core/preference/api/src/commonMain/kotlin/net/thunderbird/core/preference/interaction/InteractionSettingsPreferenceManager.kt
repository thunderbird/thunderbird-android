package net.thunderbird.core.preference.interaction

import net.thunderbird.core.preference.PreferenceManager

enum class InteractionSettingKey(val value: String) {

    UseVolumeKeysForNavigation("useVolumeKeysForNavigation"),
    MessageViewPostDeleteAction("messageViewPostDeleteAction"),
    SwipeActionLeft("swipeLeftAction"),
    SwipeActionRight("swipeRightAction"),
    ConfirmDelete("confirmDelete"),
    ConfirmDiscardMessage("confirmDiscardMessage"),
    ConfirmDeleteStarred("confirmDeleteStarred"),
    ConfirmSpam("confirmSpam"),
    ConfirmDeleteFromNotification("confirmDeleteFromNotification"),
    ConfirmMarkAllRead("confirmMarkAllRead"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface InteractionSettingsPreferenceManager : PreferenceManager<InteractionSettings>
