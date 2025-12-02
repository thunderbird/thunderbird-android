package net.thunderbird.feature.mail.message.list.ui.event

import net.thunderbird.feature.mail.message.list.preferences.ActionRequiringUserConfirmation
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.state.Folder
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

/**
 * Represents events related to refiling messages, such as archiving, deleting, moving, or copying.
 * These events are triggered by user actions in the message list UI.
 *
 * @see MessageListEvent
 */
sealed interface RefileEvent : MessageListEvent.UserEvent {
    /**
     * Event to archive one or more messages.
     *
     * @param messages The list of messages to be archived.
     */
    data class ArchiveMessages(val messages: List<MessageItemUi>) : RefileEvent {
        constructor(message: MessageItemUi) : this(messages = listOf(message))
    }

    /**
     * Event to delete a list of messages.
     *
     * @param messages The list of [MessageItemUi] to be deleted.
     */
    data class DeleteMessages(val messages: List<MessageItemUi>) : RefileEvent {
        constructor(message: MessageItemUi) : this(messages = listOf(message))
    }

    /**
     * Event to move one or more messages to a different folder.
     *
     * @param messages The list of [MessageItemUi]s to be moved.
     * @param folder The destination [Folder] where the messages will be moved.
     */
    data class MoveMessages(val messages: List<MessageItemUi>, val folder: Folder) : RefileEvent {
        constructor(message: MessageItemUi, folder: Folder) : this(messages = listOf(message), folder = folder)
    }

    /**
     * Event to copy a list of messages to a specific folder.
     *
     * @param messages The list of [MessageItemUi] to be copied.
     * @param folder The destination [Folder] to copy the messages to.
     */
    data class CopyMessages(val messages: List<MessageItemUi>, val folder: Folder) : RefileEvent {
        constructor(message: MessageItemUi, folder: Folder) : this(messages = listOf(message), folder = folder)
    }

    /**
     * Event to request user confirmation for a refile action (e.g., delete, archive).
     *
     * This is typically triggered when the user has configured the app to ask for confirmation
     * before performing certain destructive or irreversible actions.
     *
     * @param action The specific action that requires confirmation from the user.
     * @see ActionRequiringUserConfirmation
     * @see MessageListPreferences
     */
    data class ConfirmAction(val action: ActionRequiringUserConfirmation) : RefileEvent
}
