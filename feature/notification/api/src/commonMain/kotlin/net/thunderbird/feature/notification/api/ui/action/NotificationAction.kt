package net.thunderbird.feature.notification.api.ui.action

import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize

/**
 * Represents the various actions that can be performed on a notification.
 */
@KmpParcelize
sealed interface NotificationAction : KmpParcelable {
    /**
     * Action to reply to the email message associated with the notification.
     */
    data object Reply : NotificationAction

    /**
     * Action to mark the email message associated with the notification as read.
     */
    data object MarkAsRead : NotificationAction

    /**
     * Action to delete the email message associated with the notification.
     */
    data object Delete : NotificationAction

    /**
     * Action to mark the email message associated with the notification as spam.
     */
    data object MarkAsSpam : NotificationAction

    /**
     * Action to archive the email message associated with the notification.
     */
    data object Archive : NotificationAction

    /**
     * Action to prompt the user to update server settings, typically when authentication fails.
     */
    data object UpdateServerSettings : NotificationAction

    /**
     * Action to retry a failed operation, such as sending a message or fetching new messages.
     */
    data object Retry : NotificationAction

    /**
     * Represents a custom notification action.
     *
     * This can be used for actions that are not predefined and require a specific message.
     *
     * @property message The text to be displayed for this custom action.
     */
    data class CustomAction(val message: String) : NotificationAction
}
