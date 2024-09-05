package app.k9mail.feature.widget.unread

import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.notification.PushNotificationState

class FakeCoreResourceProvider : CoreResourceProvider {
    override fun searchUnifiedInboxTitle(): String = "Unified Inbox"

    override fun searchUnifiedInboxDetail(): String = "All messages in unified folders"

    override fun defaultIdentityDescription(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun contactDisplayNamePrefix(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun contactUnknownSender(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun contactUnknownRecipient(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderFrom(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderTo(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderCc(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderDate(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderSubject(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun messageHeaderSeparator(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun noSubject(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun userAgent(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun replyHeader(sender: String): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun replyHeader(sender: String, sentDate: String): String {
        throw UnsupportedOperationException("not implemented")
    }

    override val iconPushNotification: Int
        get() = throw UnsupportedOperationException("not implemented")

    override fun pushNotificationText(notificationState: PushNotificationState): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun pushNotificationInfoText(): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun pushNotificationGrantAlarmPermissionText(): String {
        throw UnsupportedOperationException("not implemented")
    }
}
