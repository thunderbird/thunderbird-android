package net.thunderbird.feature.notification.api.ui.action.icon

import net.thunderbird.feature.notification.api.R
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

internal actual val NotificationActionIcons.Reply: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_reply,
    )

internal actual val NotificationActionIcons.MarkAsRead: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_mark_email_read,
    )

internal actual val NotificationActionIcons.Delete: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_delete,
    )

internal actual val NotificationActionIcons.MarkAsSpam: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_report,
    )

internal actual val NotificationActionIcons.Archive: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_archive,
    )

internal actual val NotificationActionIcons.UpdateServerSettings: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_settings,
    )

internal actual val NotificationActionIcons.Retry: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_refresh,
    )

internal actual val NotificationActionIcons.DisablePushAction: NotificationIcon
    get() = NotificationIcon(
        systemNotificationIcon = R.drawable.ic_settings,
    )
