package net.thunderbird.feature.mail.message.list.ui.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences

/**
 * Represents the UI state for a single message item in the message list.
 *
 * This data class encapsulates all the information required to render a message item,
 * including its display state, identifiers, sender/recipient details, content snippets, and metadata.
 *
 * @param state The current display state of the message (e.g., Read, Unread, Selected).
 * @param id The unique identifier for the message.
 * @param folderId The identifier of the folder containing this message.
 * @param account The account to which this message belongs.
 * @param senders A list of identities who sent the message.
 * @param recipients A list of identities who received the message.
 * @param subject The subject line of the message.
 * @param excerpt A short snippet or preview of the message body.
 * @param formattedReceivedAt A user-friendly, formatted string representing when the message was received.
 * @param attachments A list of attachments included in the message.
 * @param starred A flag indicating whether the message is marked as starred/important.
 * @param encrypted A flag indicating whether the message is encrypted.
 * @param answered A flag indicating whether the message has been replied to.
 * @param forwarded A flag indicating whether the message has been forwarded.
 * @param conversations A list of related messages in the same conversation thread.
 *  Defaults to an empty list. Always empty if [MessageListPreferences.groupConversations] is `false`
 */
data class MessageItemUi(
    val state: State,
    val id: String,
    val folderId: String,
    val account: Account,
    val senders: ImmutableList<EmailIdentity>,
    val recipients: ImmutableList<EmailIdentity>,
    val subject: String,
    val excerpt: String,
    val formattedReceivedAt: String,
    val attachments: ImmutableList<MessageItemAttachment>,
    val starred: Boolean,
    val encrypted: Boolean,
    val answered: Boolean,
    val forwarded: Boolean,
    val selected: Boolean,
    val conversations: ImmutableList<MessageItemUi> = persistentListOf(),
) {
    /**
     * Represents the visual and interactive state of a `MessageItem`.
     *
     * This enum defines the different states a message can be in within the message item,
     * which dictates its styling and available actions.
     */
    enum class State {
        /**
         * The message is currently being displayed in the message reader pane.
         *
         * **Note:** Only available when Home screen is on Split mode.
         **/
        Active,

        /** The message has just been added to the message list. **/
        New,

        /** The message has been read by the user. */
        Read,

        /** The message has not yet been read by the user. */
        Unread,
    }
}
