package com.fsck.k9

import com.fsck.k9.notification.PushNotificationState

class TestCoreResourceProvider : CoreResourceProvider {
    override fun defaultSignature() = throw UnsupportedOperationException("not implemented")

    override fun defaultIdentityDescription() = throw UnsupportedOperationException("not implemented")

    override fun contactDisplayNamePrefix() = throw UnsupportedOperationException("not implemented")
    override fun contactUnknownSender() = throw UnsupportedOperationException("not implemented")
    override fun contactUnknownRecipient() = throw UnsupportedOperationException("not implemented")

    override fun messageHeaderFrom() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderTo() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderCc() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderDate() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderSubject() = throw UnsupportedOperationException("not implemented")
    override fun messageHeaderSeparator() = throw UnsupportedOperationException("not implemented")

    override fun noSubject() = throw UnsupportedOperationException("not implemented")

    override fun userAgent(): String = "K-9 Mail for Android"
    override fun encryptedSubject(): String = "Encrypted message"

    override fun replyHeader(sender: String) = throw UnsupportedOperationException("not implemented")
    override fun replyHeader(sender: String, sentDate: String) = throw UnsupportedOperationException("not implemented")

    override fun searchUnifiedInboxTitle() = throw UnsupportedOperationException("not implemented")
    override fun searchUnifiedInboxDetail() = throw UnsupportedOperationException("not implemented")

    override fun outboxFolderName() = throw UnsupportedOperationException("not implemented")

    override val iconPushNotification: Int
        get() = throw UnsupportedOperationException("not implemented")

    override fun pushNotificationText(notificationState: PushNotificationState): String {
        throw UnsupportedOperationException("not implemented")
    }

    override fun pushNotificationInfoText(): String = throw UnsupportedOperationException("not implemented")
    override fun pushNotificationGrantAlarmPermissionText() = throw UnsupportedOperationException("not implemented")
}
