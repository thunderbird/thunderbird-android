package com.fsck.k9.notification

import app.k9mail.legacy.account.Account
import com.fsck.k9.controller.MessageReference

internal data class NewMailNotificationData(
    val cancelNotificationIds: List<Int>,
    val baseNotificationData: BaseNotificationData,
    val singleNotificationData: List<SingleNotificationData>,
    val summaryNotificationData: SummaryNotificationData?,
)

internal data class BaseNotificationData(
    val account: Account,
    val accountName: String,
    val groupKey: String,
    val color: Int,
    val newMessagesCount: Int,
    val lockScreenNotificationData: LockScreenNotificationData,
    val appearance: NotificationAppearance,
)

internal sealed interface LockScreenNotificationData {
    object None : LockScreenNotificationData
    object AppName : LockScreenNotificationData
    object Public : LockScreenNotificationData
    object MessageCount : LockScreenNotificationData
    data class SenderNames(val senderNames: String) : LockScreenNotificationData
}

internal data class NotificationAppearance(
    val ringtone: String?,
    val vibrationPattern: LongArray?,
    val ledColor: Int?,
)

internal data class SingleNotificationData(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: NotificationContent,
    val actions: List<NotificationAction>,
    val wearActions: List<WearNotificationAction>,
    val addLockScreenNotification: Boolean,
)

internal sealed interface SummaryNotificationData

internal data class SummarySingleNotificationData(
    val singleNotificationData: SingleNotificationData,
) : SummaryNotificationData

internal data class SummaryInboxNotificationData(
    val notificationId: Int,
    val isSilent: Boolean,
    val timestamp: Long,
    val content: List<CharSequence>,
    val additionalMessagesCount: Int,
    val messageReferences: List<MessageReference>,
    val actions: List<SummaryNotificationAction>,
    val wearActions: List<SummaryWearNotificationAction>,
) : SummaryNotificationData

internal enum class NotificationAction {
    Reply,
    MarkAsRead,
    Delete,
}

internal enum class WearNotificationAction {
    Reply,
    MarkAsRead,
    Delete,
    Archive,
    Spam,
}

internal enum class SummaryNotificationAction {
    MarkAsRead,
    Delete,
}

internal enum class SummaryWearNotificationAction {
    MarkAsRead,
    Delete,
    Archive,
}
