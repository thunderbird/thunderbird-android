package com.fsck.k9.resources

import android.content.Context
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.notification.PushNotificationState
import com.fsck.k9.ui.R

class K9CoreResourceProvider(
    private val context: Context,
) : CoreResourceProvider {
    override fun defaultIdentityDescription(): String = context.getString(R.string.default_identity_description)

    override fun contactDisplayNamePrefix(): String = context.getString(R.string.message_to_label)
    override fun contactUnknownSender(): String = context.getString(R.string.unknown_sender)
    override fun contactUnknownRecipient(): String = context.getString(R.string.unknown_recipient)

    override fun messageHeaderFrom(): String = context.getString(R.string.message_compose_quote_header_from)
    override fun messageHeaderTo(): String = context.getString(R.string.message_compose_quote_header_to)
    override fun messageHeaderCc(): String = context.getString(R.string.message_compose_quote_header_cc)
    override fun messageHeaderDate(): String = context.getString(R.string.message_compose_quote_header_send_date)
    override fun messageHeaderSubject(): String = context.getString(R.string.message_compose_quote_header_subject)
    override fun messageHeaderSeparator(): String = context.getString(R.string.message_compose_quote_header_separator)

    override fun noSubject(): String = context.getString(R.string.general_no_subject)

    override fun userAgent(): String = context.getString(R.string.message_header_mua)

    override fun replyHeader(sender: String): String =
        context.getString(R.string.message_compose_reply_header_fmt, sender)

    override fun replyHeader(sender: String, sentDate: String): String =
        context.getString(R.string.message_compose_reply_header_fmt_with_date, sentDate, sender)

    override fun searchUnifiedFoldersTitle(): String = context.getString(R.string.integrated_inbox_title)
    override fun searchUnifiedFoldersDetail(): String = context.getString(R.string.integrated_inbox_detail)

    override val iconPushNotification: Int = Icons.Outlined.Notifications

    override fun pushNotificationText(notificationState: PushNotificationState): String {
        val resId = when (notificationState) {
            PushNotificationState.INITIALIZING -> R.string.push_notification_state_initializing
            PushNotificationState.LISTENING -> R.string.push_notification_state_listening
            PushNotificationState.WAIT_BACKGROUND_SYNC -> R.string.push_notification_state_wait_background_sync
            PushNotificationState.WAIT_NETWORK -> R.string.push_notification_state_wait_network
            PushNotificationState.ALARM_PERMISSION_MISSING -> R.string.push_notification_state_alarm_permission_missing
        }
        return context.getString(resId)
    }

    override fun pushNotificationInfoText(): String = context.getString(R.string.push_notification_info)

    override fun pushNotificationGrantAlarmPermissionText(): String =
        context.getString(R.string.push_notification_grant_alarm_permission)
}
