package net.thunderbird.feature.notification.receiver

import net.thunderbird.feature.notification.content.InAppNotification

/**
 * This notifier is responsible for taking a [InAppNotification] data object and
 * presenting it to the user in a suitable way.
 *
 * **Note:** The current implementation is a placeholder and needs to be completed
 * as part of GitHub Issue #9245.
 */
internal class InAppNotificationNotifier : NotificationNotifier<InAppNotification> {
    override fun show(notification: InAppNotification) {
        TODO("Implementation on GitHub Issue #9245")
    }
}
