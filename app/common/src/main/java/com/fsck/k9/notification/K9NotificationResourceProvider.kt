package com.fsck.k9.notification

import android.content.Context
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R

class K9NotificationResourceProvider(private val context: Context) : NotificationResourceProvider {
    override val iconWarning: Int = Icons.Outlined.Warning
    override val iconMarkAsRead: Int = Icons.Outlined.MarkEmailRead
    override val iconDelete: Int = Icons.Outlined.Delete
    override val iconReply: Int = Icons.Outlined.Reply
    override val iconNewMail: Int = Icons.Outlined.MarkEmailUnread
    override val iconSendingMail: Int = R.drawable.notification_icon_check_mail
    override val iconCheckingMail: Int = R.drawable.notification_icon_check_mail
    override val iconBackgroundWorkNotification: Int = R.drawable.notification_icon_background_work
    override val wearIconMarkAsRead: Int = Icons.Outlined.MarkEmailRead
    override val wearIconDelete: Int = Icons.Outlined.Delete
    override val wearIconArchive: Int = Icons.Outlined.Archive
    override val wearIconReplyAll: Int = Icons.Outlined.Reply
    override val wearIconMarkAsSpam: Int = Icons.Outlined.Report

    override val pushChannelName: String
        get() = context.getString(R.string.notification_channel_push_title)
    override val pushChannelDescription: String
        get() = context.getString(R.string.notification_channel_push_description)
    override val messagesChannelName: String
        get() = context.getString(R.string.notification_channel_messages_title)
    override val messagesChannelDescription: String
        get() = context.getString(R.string.notification_channel_messages_description)
    override val miscellaneousChannelName: String
        get() = context.getString(R.string.notification_channel_miscellaneous_title)
    override val miscellaneousChannelDescription: String
        get() = context.getString(R.string.notification_channel_miscellaneous_description)

    override fun authenticationErrorTitle(): String =
        context.getString(R.string.notification_authentication_error_title)

    override fun authenticationErrorBody(accountName: String): String =
        context.getString(R.string.notification_authentication_error_text, accountName)

    override fun notifyErrorTitle(): String = context.getString(R.string.notification_notify_error_title)

    override fun notifyErrorText(): String = context.getString(R.string.notification_notify_error_text)

    override fun certificateErrorTitle(): String = context.getString(R.string.notification_certificate_error_public)

    override fun certificateErrorTitle(accountName: String): String =
        context.getString(R.string.notification_certificate_error_title, accountName)

    override fun certificateErrorBody(): String = context.getString(R.string.notification_certificate_error_text)

    override fun newMessagesTitle(newMessagesCount: Int): String {
        return context.resources.getQuantityString(
            R.plurals.notification_new_messages_title,
            newMessagesCount,
            newMessagesCount,
        )
    }

    override fun additionalMessages(overflowMessagesCount: Int, accountName: String): String =
        context.getString(R.string.notification_additional_messages, overflowMessagesCount, accountName)

    override fun previewEncrypted(): String = context.getString(R.string.preview_encrypted)

    override fun noSubject(): String = context.getString(R.string.general_no_subject)

    override fun recipientDisplayName(recipientDisplayName: String): String =
        context.getString(R.string.message_to_fmt, recipientDisplayName)

    override fun noSender(): String = context.getString(R.string.general_no_sender)

    override fun sendFailedTitle(): String = context.getString(R.string.send_failure_subject)

    override fun sendingMailTitle(): String = context.getString(R.string.notification_bg_send_title)

    override fun sendingMailBody(accountName: String): String =
        context.getString(R.string.notification_bg_send_ticker, accountName)

    override fun checkingMailTicker(accountName: String, folderName: String): String =
        context.getString(R.string.notification_bg_sync_ticker, accountName, folderName)

    override fun checkingMailTitle(): String = context.getString(R.string.notification_bg_sync_title)

    override fun checkingMailSeparator(): String = context.getString(R.string.notification_bg_title_separator)

    override fun actionMarkAsRead(): String = context.getString(R.string.notification_action_mark_as_read)

    override fun actionMarkAllAsRead(): String = context.getString(R.string.notification_action_mark_all_as_read)

    override fun actionDelete(): String = context.getString(R.string.notification_action_delete)

    override fun actionDeleteAll(): String = context.getString(R.string.notification_action_delete_all)

    override fun actionReply(): String = context.getString(R.string.notification_action_reply)

    override fun actionArchive(): String = context.getString(R.string.notification_action_archive)

    override fun actionArchiveAll(): String = context.getString(R.string.notification_action_archive_all)

    override fun actionMarkAsSpam(): String = context.getString(R.string.notification_action_spam)
}
