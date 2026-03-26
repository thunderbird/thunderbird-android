package net.thunderbird.feature.mail.message.list.ui.state

import androidx.compose.runtime.Immutable

/**
 * Represents the UI state for a single message item in the message list.
 *
 * This data class encapsulates all the information required to render a message item,
 * including its display state, identifiers, sender/recipient details, content snippets, and metadata.
 *
 * @property state The current display state of the message (e.g., Read, Unread, Selected).
 * @property id The unique identifier for the message.
 * @property folderId The identifier of the folder containing this message.
 * @property account The account to which this message belongs.
 * @property senders The composed representation of the message sender(s) with display name,
 *  styling, and avatar.
 * @property subject The subject line of the message.
 * @property excerpt A short snippet or preview of the message body.
 * @property formattedReceivedAt A user-friendly, formatted string representing when the message was received.
 * @property hasAttachments Whether the message contains one or more attachments.
 * @property starred A flag indicating whether the message is marked as starred/important.
 * @property encrypted A flag indicating whether the message is encrypted.
 * @property answered A flag indicating whether the message has been replied to.
 * @property forwarded A flag indicating whether the message has been forwarded.
 * @property threadCount The number of messages in the thread. A value of 0-1 indicates a single
 *  message (not threaded).
 * @property isActive A flag indicating whether the message is currently active. Defaults to `false`.
 *  **NOTE:** Only available when Home Screen is on Split mode.
 */
@Immutable
data class MessageItemUi(
    val state: State,
    val id: String,
    val folderId: String,
    val account: Account,
    val senders: ComposedAddressUi,
    val subject: String,
    val excerpt: String,
    val formattedReceivedAt: String,
    val hasAttachments: Boolean,
    val starred: Boolean,
    val encrypted: Boolean,
    val answered: Boolean,
    val forwarded: Boolean,
    val selected: Boolean,
    val threadCount: Int = 0,
    val isActive: Boolean = false,
) {
    /**
     * Represents the visual and interactive state of a `MessageItem`.
     *
     * This enum defines the different states a message can be in within the message item,
     * which dictates its styling and available actions.
     */
    enum class State {
        /** The message has just been added to the message list. **/
        New,

        /** The message has been read by the user. */
        Read,

        /** The message has not yet been read by the user. */
        Unread,
    }
}
