package net.thunderbird.feature.notification.testing.fake.receiver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.content.Notification
import net.thunderbird.feature.notification.api.receiver.InAppNotificationStream
import net.thunderbird.feature.notification.api.receiver.NotificationStream

abstract class BaseFakeNotificationStream<TNotification : Notification>(
    initialNotifications: Set<TNotification>,
) : NotificationStream<TNotification> {
    private val _notifications = MutableStateFlow(initialNotifications)

    override val notifications: StateFlow<Set<TNotification>> = _notifications.asStateFlow()

    fun addNotification(notification: TNotification) {
        _notifications.update { it + notification }
    }

    fun removeNotification(notification: TNotification) {
        _notifications.update { it - notification }
    }
}

class FakeInAppNotificationStream(
    initialNotifications: Set<InAppNotification> = emptySet(),
) : BaseFakeNotificationStream<InAppNotification>(initialNotifications), InAppNotificationStream
