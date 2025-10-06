package net.thunderbird.feature.notification.testing.fake

import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.AppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.testing.fake.icon.EMPTY_SYSTEM_NOTIFICATION_ICON

data class FakeSystemOnlyNotification(
    override val title: String = "fake title",
    override val contentText: String? = "fake content",
    override val severity: NotificationSeverity = NotificationSeverity.Information,
    override val icon: NotificationIcon = NotificationIcon(
        systemNotificationIcon = EMPTY_SYSTEM_NOTIFICATION_ICON,
    ),
    override val channel: NotificationChannel = NotificationChannel.Messages(
        accountUuid = "",
        suffix = "",
    ),
) : AppNotification(), SystemNotification
