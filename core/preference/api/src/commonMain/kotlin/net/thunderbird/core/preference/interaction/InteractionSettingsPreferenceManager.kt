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
    MessageViewPostMarkAsRead("messageViewPostMarkAsReadAction"),
}

interface InteractionSettingsPreferenceManager : PreferenceManager<InteractionSettings>
