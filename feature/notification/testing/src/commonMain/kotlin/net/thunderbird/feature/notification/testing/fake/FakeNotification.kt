package net.thunderbird.feature.notification.testing.fake

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.AppNotification
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.testing.fake.icon.EMPTY_SYSTEM_NOTIFICATION_ICON

data class FakeNotification(
    override val title: String = "fake title",
    override val contentText: String? = "fake content",
    override val severity: NotificationSeverity = NotificationSeverity.Information,
    override val icon: NotificationIcon = NotificationIcon(
        systemNotificationIcon = EMPTY_SYSTEM_NOTIFICATION_ICON,
        inAppNotificationIcon = ImageVector.Builder(
            defaultWidth = 0.dp,
            defaultHeight = 0.dp,
            viewportWidth = 0f,
            viewportHeight = 0f,
        ).build(),
    ),
    override val channel: NotificationChannel = NotificationChannel.Messages(
        accountUuid = "",
        suffix = "",
    ),
) : AppNotification(), SystemNotification, InAppNotification
