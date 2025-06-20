package net.thunderbird.feature.notification.api.content

import net.thunderbird.core.common.exception.rootCauseMassage
import net.thunderbird.core.common.io.KmpIgnoredOnParcel
import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.api.LockscreenNotificationAppearance
import net.thunderbird.feature.notification.api.NotificationChannel
import net.thunderbird.feature.notification.api.NotificationGroup
import net.thunderbird.feature.notification.api.NotificationSeverity
import net.thunderbird.feature.notification.api.ui.NotificationStyle
import net.thunderbird.feature.notification.api.ui.action.NotificationAction
import net.thunderbird.feature.notification.api.ui.builder.notificationStyle
import net.thunderbird.feature.notification.api.ui.icon.MailFetching
import net.thunderbird.feature.notification.api.ui.icon.MailSendFailed
import net.thunderbird.feature.notification.api.ui.icon.MailSending
import net.thunderbird.feature.notification.api.ui.icon.NewMailSingleMail
import net.thunderbird.feature.notification.api.ui.icon.NewMailSummaryMail
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcons
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_additional_messages
import net.thunderbird.feature.notification.resources.notification_bg_send_ticker
import net.thunderbird.feature.notification.resources.notification_bg_send_title
import net.thunderbird.feature.notification.resources.notification_bg_sync_text
import net.thunderbird.feature.notification.resources.notification_bg_sync_ticker
import net.thunderbird.feature.notification.resources.notification_bg_sync_title
import net.thunderbird.feature.notification.resources.notification_new_messages_title
import net.thunderbird.feature.notification.resources.send_failure_subject
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Represents mail-related notifications. By default, all mail-related subclasses are [SystemNotification],
 * however they may also implement [InAppNotification] for more severe notifications.
 */
@KmpParcelize
sealed class MailNotification : AppNotification(), SystemNotification {
    @KmpIgnoredOnParcel
    override val severity: NotificationSeverity = NotificationSeverity.Information

    @KmpIgnoredOnParcel
    override val authenticationRequired: Boolean = true

    @KmpParcelize
    @ConsistentCopyVisibility
    data class Fetching private constructor(
        override val accountNumber: Int,
        override val title: String,
        override val accessibilityText: String,
        override val contentText: String?,
        override val channel: NotificationChannel,
        @KmpIgnoredOnParcel
        override val icon: NotificationIcon = NotificationIcons.MailFetching,
    ) : MailNotification() {
        @KmpIgnoredOnParcel
        override val lockscreenNotification: SystemNotification get() = copy(contentText = null)

        companion object {
            /**
             * Creates a [Fetching] notification.
             *
             * @param accountUuid The UUID of the account being fetched.
             * @param accountDisplayName The display name of the account being fetched.
             * @param folderName The name of the folder being fetched, or null if fetching all folders.
             * @return A [Fetching] notification.
             */
            suspend operator fun invoke(
                accountNumber: Int,
                accountUuid: String,
                accountDisplayName: String,
                folderName: String?,
            ): Fetching {
                val title = getString(resource = Res.string.notification_bg_sync_title)
                return Fetching(
                    accountNumber = accountNumber,
                    title = title,
                    accessibilityText = folderName?.let { folderName ->
                        getString(
                            resource = Res.string.notification_bg_sync_ticker,
                            accountDisplayName,
                            folderName,
                        )
                    } ?: title,
                    contentText = folderName?.let { folderName ->
                        getString(
                            resource = Res.string.notification_bg_sync_text,
                            accountDisplayName,
                            folderName,
                        )
                    } ?: accountDisplayName,
                    channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
                )
            }
        }
    }

    @KmpParcelize
    @ConsistentCopyVisibility
    data class Sending private constructor(
        override val accountNumber: Int,
        override val title: String,
        override val accessibilityText: String,
        override val contentText: String?,
        override val channel: NotificationChannel,
        @KmpIgnoredOnParcel
        override val icon: NotificationIcon = NotificationIcons.MailSending,
    ) : MailNotification() {
        @KmpIgnoredOnParcel
        override val lockscreenNotification: SystemNotification get() = copy(contentText = null)

        companion object {
            /**
             * Creates a [Sending] notification.
             *
             * @param accountUuid The UUID of the account sending the message.
             * @param accountDisplayName The display name of the account sending the message.
             * @return A [Sending] notification.
             */
            suspend operator fun invoke(
                accountNumber: Int,
                accountUuid: String,
                accountDisplayName: String,
            ): Sending = Sending(
                accountNumber = accountNumber,
                title = getString(resource = Res.string.notification_bg_send_title),
                accessibilityText = getString(
                    resource = Res.string.notification_bg_send_ticker,
                    accountDisplayName,
                ),
                contentText = accountDisplayName,
                channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
            )
        }
    }

    @KmpParcelize
    @ConsistentCopyVisibility
    data class SendFailed private constructor(
        override val accountNumber: Int,
        override val title: String,
        override val contentText: String?,
        override val channel: NotificationChannel,
        @KmpIgnoredOnParcel
        override val icon: NotificationIcon = NotificationIcons.MailSendFailed,
    ) : MailNotification(), InAppNotification {
        @KmpIgnoredOnParcel
        override val severity: NotificationSeverity = NotificationSeverity.Critical

        @KmpIgnoredOnParcel
        override val lockscreenNotification: SystemNotification get() = copy(contentText = null)

        @KmpIgnoredOnParcel
        override val actions: Set<NotificationAction> = setOf(
            NotificationAction.Retry,
        )

        companion object {
            /**
             * Creates a [SendFailed] notification.
             *
             * @param accountUuid The UUID of the account sending the message.
             * @param exception The exception that occurred during sending.
             * @return A [SendFailed] notification.
             */
            suspend operator fun invoke(
                accountNumber: Int,
                accountUuid: String,
                exception: Exception,
            ): SendFailed = SendFailed(
                accountNumber = accountNumber,
                title = getString(resource = Res.string.send_failure_subject),
                contentText = exception.rootCauseMassage,
                channel = NotificationChannel.Miscellaneous(accountUuid = accountUuid),
            )
        }
    }

    /**
     * Represents a notification for new mail.
     *
     * @property accountUuid The UUID of the account associated with this notification.
     * @property messagesNotificationChannelSuffix The suffix for the notification channel.
     * @property channel The notification channel for this notification.
     * @property actions The set of actions available for this notification.
     */
    @KmpParcelize
    sealed class NewMail : MailNotification() {
        abstract val accountUuid: String
        abstract val messagesNotificationChannelSuffix: String

        @KmpIgnoredOnParcel
        override val channel: NotificationChannel = NotificationChannel.Messages(
            accountUuid = accountUuid,
            suffix = messagesNotificationChannelSuffix,
        )

        @KmpIgnoredOnParcel
        override val actions: Set<NotificationAction> = setOf(
            NotificationAction.Reply,
            NotificationAction.MarkAsRead,
            NotificationAction.Delete,
            NotificationAction.Archive,
            NotificationAction.MarkAsSpam,
        )

        /**
         * Represents a notification for a single new email.
         *
         * @property accountUuid The UUID of the account that received the email.
         * @property accountName The display name of the account that received the email.
         * @property messagesNotificationChannelSuffix The suffix for the messages notification channel.
         * @property summary A short summary of the email content.
         * @property sender The sender of the email.
         * @property subject The subject of the email.
         * @property preview A preview of the email content.
         * @property group The notification group this notification belongs to, if any.
         * @property lockscreenNotificationAppearance Specifies how this notification should appear on the lockscreen.
         */
        @KmpParcelize
        data class SingleMail(
            override val accountNumber: Int,
            override val accountUuid: String,
            val accountName: String,
            override val messagesNotificationChannelSuffix: String,
            val summary: String,
            val sender: String,
            val subject: String,
            val preview: String,
            override val group: NotificationGroup?,
            override val lockscreenNotificationAppearance: LockscreenNotificationAppearance,
            @KmpIgnoredOnParcel
            override val icon: NotificationIcon = NotificationIcons.NewMailSingleMail,
        ) : NewMail() {
            @KmpIgnoredOnParcel
            override val title: String = sender

            @KmpIgnoredOnParcel
            override val contentText: String = subject

            @KmpIgnoredOnParcel
            override val systemNotificationStyle: NotificationStyle.System = notificationStyle {
                systemStyle {
                    bigText(preview)
                }
            }.systemStyle
        }

        /**
         * Represents a summary notification for new mail.
         *
         * @property accountUuid The UUID of the account.
         * @property accountName The display name of the account.
         * @property messagesNotificationChannelSuffix The suffix for the messages notification channel.
         * @property title The title of the notification.
         * @property contentText The content text of the notification, or null if there is no content text.
         * @property group The notification group this summary belongs to.
         */
        @KmpParcelize
        @ConsistentCopyVisibility
        data class SummaryMail private constructor(
            override val accountNumber: Int,
            override val accountUuid: String,
            val accountName: String,
            override val messagesNotificationChannelSuffix: String,
            override val title: String,
            override val contentText: String?,
            override val group: NotificationGroup,
            @KmpIgnoredOnParcel
            override val icon: NotificationIcon = NotificationIcons.NewMailSummaryMail,
        ) : NewMail() {
            companion object {
                /**
                 * Creates a [SummaryMail] notification.
                 *
                 * @param accountUuid The UUID of the account.
                 * @param accountDisplayName The display name of the account.
                 * @param messagesNotificationChannelSuffix The suffix for the messages notification channel.
                 * @param newMessageCount The number of new messages.
                 * @param additionalMessagesCount The number of additional messages (not shown in individual
                 * notifications).
                 * @param group The notification group this summary belongs to.
                 * @return A [SummaryMail] notification.
                 */
                suspend operator fun invoke(
                    accountNumber: Int,
                    accountUuid: String,
                    accountDisplayName: String,
                    messagesNotificationChannelSuffix: String,
                    newMessageCount: Int,
                    additionalMessagesCount: Int,
                    group: NotificationGroup,
                ): SummaryMail = SummaryMail(
                    accountNumber = accountNumber,
                    accountUuid = accountUuid,
                    accountName = accountDisplayName,
                    messagesNotificationChannelSuffix = messagesNotificationChannelSuffix,
                    title = getPluralString(
                        Res.plurals.notification_new_messages_title,
                        newMessageCount,
                        newMessageCount,
                    ),
                    contentText = if (additionalMessagesCount > 0) {
                        getString(
                            Res.string.notification_additional_messages,
                            additionalMessagesCount,
                            accountDisplayName,
                        )
                    } else {
                        accountDisplayName
                    },
                    group = group,
                )
            }
        }
    }
}
