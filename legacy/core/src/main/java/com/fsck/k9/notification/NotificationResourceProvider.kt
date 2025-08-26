package com.fsck.k9.notification

import android.graphics.Bitmap
import com.fsck.k9.mail.Address

interface NotificationResourceProvider {
    val iconWarning: Int
    val iconMarkAsRead: Int
    val iconDelete: Int
    val iconReply: Int
    val iconNewMail: Int
    val iconSendingMail: Int
    val iconCheckingMail: Int
    val iconBackgroundWorkNotification: Int
    val wearIconMarkAsRead: Int
    val wearIconDelete: Int
    val wearIconArchive: Int
    val wearIconReplyAll: Int
    val wearIconMarkAsSpam: Int

    val pushChannelName: String
    val pushChannelDescription: String
    val messagesChannelName: String
    val messagesChannelDescription: String
    val miscellaneousChannelName: String
    val miscellaneousChannelDescription: String

    fun authenticationErrorTitle(): String
    fun authenticationErrorBody(accountName: String): String

    suspend fun avatar(address: Address): Bitmap?

    fun notifyErrorTitle(): String
    fun notifyErrorText(): String

    fun certificateErrorTitle(): String
    fun certificateErrorTitle(accountName: String): String
    fun certificateErrorBody(): String

    fun newMessagesTitle(newMessagesCount: Int): String
    fun additionalMessages(overflowMessagesCount: Int, accountName: String): String
    fun previewEncrypted(): String
    fun noSubject(): String
    fun recipientDisplayName(recipientDisplayName: String): String
    fun noSender(): String

    fun sendFailedTitle(): String
    fun sendingMailTitle(): String
    fun sendingMailBody(accountName: String): String

    fun checkingMailTicker(accountName: String, folderName: String): String
    fun checkingMailTitle(): String
    fun checkingMailSeparator(): String

    fun actionMarkAsRead(): String
    fun actionMarkAllAsRead(): String
    fun actionDelete(): String
    fun actionDeleteAll(): String
    fun actionReply(): String
    fun actionArchive(): String
    fun actionArchiveAll(): String
    fun actionMarkAsSpam(): String
}
