package net.thunderbird.feature.notification.testing.fake.ui.action

import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.testing.fake.icon.EMPTY_SYSTEM_NOTIFICATION_ICON

fun createFakeNotificationAction(title: String = "fake action"): NotificationAction.CustomAction =
    NotificationAction.CustomAction(
        title = title,
        icon = NotificationIcon(
            systemNotificationIcon = EMPTY_SYSTEM_NOTIFICATION_ICON,
        ),
    )
