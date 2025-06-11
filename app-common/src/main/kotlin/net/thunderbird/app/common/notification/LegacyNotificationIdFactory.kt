package net.thunderbird.app.common.notification

import com.fsck.k9.notification.NotificationIds
import net.thunderbird.feature.notification.api.NotificationId
import net.thunderbird.feature.notification.api.NotificationIdFactory

class LegacyNotificationIdFactory : NotificationIdFactory {
    override fun next(
        accountNumber: Int,
        offset: Int,
    ): NotificationId {
        return NotificationId(
            NotificationIds.getBaseNotificationId(accountNumber) + offset,
        )
    }
}
