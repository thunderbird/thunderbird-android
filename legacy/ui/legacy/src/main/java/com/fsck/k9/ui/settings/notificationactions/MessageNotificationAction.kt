package com.fsck.k9.ui.settings.notificationactions

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.ui.R
import net.thunderbird.core.common.notification.NotificationActionTokens

/**
 * Actions available for message notifications in the settings UI.
 * Tokens map to persisted preference strings.
 */
internal enum class MessageNotificationAction(
    val token: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Reply(
        token = NotificationActionTokens.REPLY,
        labelRes = R.string.notification_action_reply,
        iconRes = Icons.Outlined.Reply,
    ),
    MarkAsRead(
        token = NotificationActionTokens.MARK_AS_READ,
        labelRes = R.string.notification_action_mark_as_read,
        iconRes = Icons.Outlined.MarkEmailRead,
    ),
    Delete(
        token = NotificationActionTokens.DELETE,
        labelRes = R.string.notification_action_delete,
        iconRes = Icons.Outlined.Delete,
    ),
    Star(
        token = NotificationActionTokens.STAR,
        labelRes = R.string.notification_action_star,
        iconRes = Icons.Outlined.Star,
    ),
    Archive(
        token = NotificationActionTokens.ARCHIVE,
        labelRes = R.string.notification_action_archive,
        iconRes = Icons.Outlined.Archive,
    ),
    Spam(
        token = NotificationActionTokens.SPAM,
        labelRes = R.string.notification_action_spam,
        iconRes = Icons.Outlined.Report,
    ),
    ;

    companion object {
        fun fromToken(token: String): MessageNotificationAction? {
            return entries.firstOrNull { it.token == token }
        }

        fun defaultOrder(): List<MessageNotificationAction> = listOf(
            Reply,
            MarkAsRead,
            Delete,
            Star,
            Archive,
            Spam,
        )
    }
}
