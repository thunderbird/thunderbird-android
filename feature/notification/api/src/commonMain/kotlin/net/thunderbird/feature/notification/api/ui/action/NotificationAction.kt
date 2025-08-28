package net.thunderbird.feature.notification.api.ui.action

import net.thunderbird.feature.notification.api.content.SystemNotification
import net.thunderbird.feature.notification.api.ui.action.icon.Archive
import net.thunderbird.feature.notification.api.ui.action.icon.Delete
import net.thunderbird.feature.notification.api.ui.action.icon.MarkAsRead
import net.thunderbird.feature.notification.api.ui.action.icon.MarkAsSpam
import net.thunderbird.feature.notification.api.ui.action.icon.NotificationActionIcons
import net.thunderbird.feature.notification.api.ui.action.icon.Reply
import net.thunderbird.feature.notification.api.ui.action.icon.Retry
import net.thunderbird.feature.notification.api.ui.action.icon.UpdateServerSettings
import net.thunderbird.feature.notification.api.ui.icon.NotificationIcon
import net.thunderbird.feature.notification.resources.api.Res
import net.thunderbird.feature.notification.resources.api.notification_action_archive
import net.thunderbird.feature.notification.resources.api.notification_action_delete
import net.thunderbird.feature.notification.resources.api.notification_action_mark_as_read
import net.thunderbird.feature.notification.resources.api.notification_action_reply
import net.thunderbird.feature.notification.resources.api.notification_action_retry
import net.thunderbird.feature.notification.resources.api.notification_action_spam
import net.thunderbird.feature.notification.resources.api.notification_action_update_server_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Represents the various actions that can be performed on a notification.
 */
sealed class NotificationAction {
    abstract val icon: NotificationIcon?
    protected abstract val titleResource: StringResource?

    open suspend fun resolveTitle(): String? = titleResource?.let { getString(it) }

    /**
     * Action to open the notification. This is the default action when a notification is tapped.
     *
     * This action typically does not have an icon or title displayed on the notification itself,
     * as it's implied by tapping the notification content.
     *
     * All [SystemNotification] will have this action implicitly, even if not specified in the
     * [SystemNotification.actions] set.
     */
    data object Tap : NotificationAction() {
        override val icon: NotificationIcon? = null
        override val titleResource: StringResource? = null
    }

    /**
     * Action to reply to the email message associated with the notification.
     */
    data object Reply : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.Reply

        override val titleResource: StringResource = Res.string.notification_action_reply
    }

    /**
     * Action to mark the email message associated with the notification as read.
     */
    data object MarkAsRead : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.MarkAsRead

        override val titleResource: StringResource = Res.string.notification_action_mark_as_read
    }

    /**
     * Action to delete the email message associated with the notification.
     */
    data object Delete : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.Delete

        override val titleResource: StringResource = Res.string.notification_action_delete
    }

    /**
     * Action to mark the email message associated with the notification as spam.
     */
    data object MarkAsSpam : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.MarkAsSpam

        override val titleResource: StringResource = Res.string.notification_action_spam
    }

    /**
     * Action to archive the email message associated with the notification.
     */
    data object Archive : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.Archive

        override val titleResource: StringResource = Res.string.notification_action_archive
    }

    /**
     * Action to prompt the user to update server settings, typically when authentication fails.
     */
    data object UpdateServerSettings : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.UpdateServerSettings

        override val titleResource: StringResource = Res.string.notification_action_update_server_settings
    }

    /**
     * Action to retry a failed operation, such as sending a message or fetching new messages.
     */
    data object Retry : NotificationAction() {
        override val icon: NotificationIcon = NotificationActionIcons.Retry

        override val titleResource: StringResource = Res.string.notification_action_retry
    }

    /**
     * Represents a custom notification action.
     *
     * This can be used for actions that are not predefined and require a specific message.
     *
     * @property title The text to be displayed for this custom action.
     */
    data class CustomAction(
        val title: String,
        override val icon: NotificationIcon? = null,
    ) : NotificationAction() {
        override val titleResource: StringResource get() = error("Custom Action must not supply a title resource")

        override suspend fun resolveTitle(): String = title
    }
}
