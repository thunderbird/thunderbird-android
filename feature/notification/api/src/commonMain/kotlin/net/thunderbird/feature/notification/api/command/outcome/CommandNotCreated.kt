package net.thunderbird.feature.notification.api.command.outcome

import net.thunderbird.feature.notification.api.command.NotificationCommand
import net.thunderbird.feature.notification.api.content.Notification

data class CommandNotCreated<out TNotification : Notification>(
    val message: String,
) : Failure<TNotification> {
    override val command: NotificationCommand<out TNotification>? = null
}
