package net.thunderbird.feature.debug.settings.notification

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.AppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

class PushIconTestNotification(
    pushIcon: Int,
) : AppNotification(), SystemNotification {
    override val title: String = "Push Icon Test"
    override val contentText: String = "Verifying NotificationIconResourceProvider"
    override val severity: NotificationSeverity = NotificationSeverity.Information
    override val channel: NotificationChannel = NotificationChannel.PushService
    override val icon: NotificationIcon = NotificationIcon(systemNotificationIcon = pushIcon)
}
