package com.fsck.k9

import com.fsck.k9.notification.PushNotificationState

interface CoreResourceProvider {
    fun defaultIdentityDescription(): String

    fun contactDisplayNamePrefix(): String
    fun contactUnknownSender(): String
    fun contactUnknownRecipient(): String

    fun messageHeaderFrom(): String
    fun messageHeaderTo(): String
    fun messageHeaderCc(): String
    fun messageHeaderDate(): String
    fun messageHeaderSubject(): String
    fun messageHeaderSeparator(): String

    fun noSubject(): String
    fun userAgent(): String

    fun replyHeader(sender: String): String
    fun replyHeader(sender: String, sentDate: String): String

    fun searchUnifiedInboxTitle(): String
    fun searchUnifiedInboxDetail(): String

    val iconPushNotification: Int
    fun pushNotificationText(notificationState: PushNotificationState): String
    fun pushNotificationInfoText(): String
    fun pushNotificationGrantAlarmPermissionText(): String
}
