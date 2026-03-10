package net.thunderbird.feature.mail.message.list.preferences

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import net.thunderbird.core.preference.display.visualSettings.message.list.UiDensity

/**
 * Represents the user's preferences for the message list view.
 *
 * This data class holds various settings that control the appearance and behavior of the message list,
 * such as layout density, conversation grouping, and display options for individual messages.
 *
 * @property density The visual density of the message list items (e.g., compact, default).
 * @property groupConversations Whether to group messages by conversation.
 * @property showCorrespondentNames Whether to display the names of correspondents.
 * @property showMessageAvatar Whether to display the contact's avatar.
 * @property showFavouriteButton Whether to display a button to mark a message as a favourite (starred).
 * @property senderAboveSubject Whether to display sender information above the subject.
 * @property excerptLines The number of lines to show for a message excerpt.
 * @property dateTimeFormat The format for displaying the date and time of messages.
 * @property useVolumeKeyNavigation Whether to enable navigating between messages using the volume keys.
 * @property serverSearchLimit The maximum number of results to fetch when performing a server search.
 * @property actionRequiringUserConfirmation A set of actions that require a confirmation dialog before execution.
 * @property colorizeBackgroundWhenRead Whether to colorize the background of read messages.
 */
data class MessageListPreferences(
    val density: UiDensity,
    val groupConversations: Boolean,
    val showCorrespondentNames: Boolean,
    val showMessageAvatar: Boolean,
    val showFavouriteButton: Boolean,
    val senderAboveSubject: Boolean,
    val excerptLines: Int,
    val dateTimeFormat: MessageListDateTimeFormat,
    val useVolumeKeyNavigation: Boolean,
    val serverSearchLimit: Int,
    val actionRequiringUserConfirmation: ImmutableSet<ActionRequiringUserConfirmation> = persistentSetOf(),
    val colorizeBackgroundWhenRead: Boolean = false,
)

/**
 * Defines actions that can be configured to require user confirmation before being executed.
 * This allows users to prevent accidental operations like deleting important messages.
 */
enum class ActionRequiringUserConfirmation {
    Delete,
    DeleteStarred,
    DeleteFromNotification,
    DiscardMessage,
    Spam,
    MarkAllRead,
}
