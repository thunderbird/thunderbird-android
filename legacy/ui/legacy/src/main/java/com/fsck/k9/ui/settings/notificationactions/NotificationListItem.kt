package com.fsck.k9.ui.settings.notificationactions

internal sealed interface NotificationListItem {
    data class Action(
        val action: MessageNotificationAction,
    ) : NotificationListItem

    data object Cutoff : NotificationListItem
}

internal val NotificationListItem.key: String
    get() = when (this) {
        is NotificationListItem.Action -> "action:${action.token}"
        NotificationListItem.Cutoff -> "cutoff"
    }
