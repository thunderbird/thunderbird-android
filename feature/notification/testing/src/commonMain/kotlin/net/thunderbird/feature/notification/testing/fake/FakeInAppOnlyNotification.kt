package net.thunderbird.feature.notification.testing.fake

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.content.AppNotification
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon

data class FakeInAppOnlyNotification(
    override val title: String = "fake title",
    override val contentText: String? = "fake content",
    override val severity: NotificationSeverity = NotificationSeverity.Information,
    override val icon: NotificationIcon = NotificationIcon(
        inAppNotificationIcon = ImageVector.Builder(
            defaultWidth = 0.dp,
            defaultHeight = 0.dp,
            viewportWidth = 0f,
            viewportHeight = 0f,
        ).build(),
    ),
) : AppNotification(), InAppNotification
