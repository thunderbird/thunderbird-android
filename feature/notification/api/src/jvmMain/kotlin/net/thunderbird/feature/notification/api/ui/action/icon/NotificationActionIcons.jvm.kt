package net.thunderbird.feature.notification.api.ui.action.icon

import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

private const val ERROR_MESSAGE = "Can't send notifications from a jvm library. Use android library or app instead."

internal actual val NotificationActionIcons.Reply: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.MarkAsRead: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.Delete: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.MarkAsSpam: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.Archive: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.UpdateServerSettings: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.Retry: NotificationIcon get() = error(ERROR_MESSAGE)
internal actual val NotificationActionIcons.DisablePushAction: NotificationIcon get() = error(ERROR_MESSAGE)
