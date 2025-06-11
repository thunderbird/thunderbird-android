package net.thunderbird.feature.notification.impl.receiver

import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.receiver.NotificationNotifier

/**
 * This notifier is responsible for taking a [SystemNotification] data object and
 * presenting it to the user in a suitable way.
 *
 * **Note:** The current implementation is a placeholder and needs to be completed
 * as part of GitHub Issue #9245.
 */
internal class SystemNotificationNotifier : NotificationNotifier<SystemNotification> {
    override fun show(id: NotificationId, notification: SystemNotification) {
    }

    override fun dispose() {
    }
}
