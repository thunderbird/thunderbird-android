package net.thunderbird.feature.notification.api.ui.action

import net.thunderbird.core.common.io.KmpIgnoredOnParcel
import net.thunderbird.core.common.io.KmpParcelable
import net.thunderbird.core.common.io.KmpParcelize
import net.thunderbird.feature.notification.resources.Res
import net.thunderbird.feature.notification.resources.notification_action_archive
import net.thunderbird.feature.notification.resources.notification_action_delete
import net.thunderbird.feature.notification.resources.notification_action_mark_as_read
import net.thunderbird.feature.notification.resources.notification_action_reply
import net.thunderbird.feature.notification.resources.notification_action_retry
import net.thunderbird.feature.notification.resources.notification_action_spam
import net.thunderbird.feature.notification.resources.notification_action_update_server_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

/**
 * Represents the various actions that can be performed on a notification.
 */
@KmpParcelize
sealed interface NotificationAction : KmpParcelable {
    val titleResource: StringResource

    suspend fun resolve(): String = getString(Reply.titleResource)

    /**
     * Action to reply to the email message associated with the notification.
     */
    data object Reply : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_reply
    }

    /**
     * Action to mark the email message associated with the notification as read.
     */
    data object MarkAsRead : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_mark_as_read
    }

    /**
     * Action to delete the email message associated with the notification.
     */
    data object Delete : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_delete
    }

    /**
     * Action to mark the email message associated with the notification as spam.
     */
    data object MarkAsSpam : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_spam
    }

    /**
     * Action to archive the email message associated with the notification.
     */
    data object Archive : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_archive
    }

    /**
     * Action to prompt the user to update server settings, typically when authentication fails.
     */
    data object UpdateServerSettings : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_update_server_settings
    }

    /**
     * Action to retry a failed operation, such as sending a message or fetching new messages.
     */
    data object Retry : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource = Res.string.notification_action_retry
    }

    /**
     * Represents a custom notification action.
     *
     * This can be used for actions that are not predefined and require a specific message.
     *
     * @property title The text to be displayed for this custom action.
     */
    data class CustomAction(val title: String) : NotificationAction {
        @KmpIgnoredOnParcel
        override val titleResource: StringResource get() = error("Custom Action must not supply a title resource")

        override suspend fun resolve(): String = title
    }
}
