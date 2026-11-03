package net.thunderbird.core.preference.interaction

import net.thunderbird.core.preference.PreferenceManager

enum class InteractionSettingKey(val value: String) {

    UseVolumeKeysForNavigation("useVolumeKeysForNavigation"),
    KeypadNavigationEnabled("isKeypadNavigationEnabled"),
    KeypadDigit0Action("keypadDigit0Action"),
    KeypadDigit1Action("keypadDigit1Action"),
    KeypadDigit2Action("keypadDigit2Action"),
    KeypadDigit3Action("keypadDigit3Action"),
    KeypadDigit4Action("keypadDigit4Action"),
    KeypadDigit5Action("keypadDigit5Action"),
    KeypadDigit6Action("keypadDigit6Action"),
    KeypadDigit7Action("keypadDigit7Action"),
    KeypadDigit8Action("keypadDigit8Action"),
    KeypadDigit9Action("keypadDigit9Action"),
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
