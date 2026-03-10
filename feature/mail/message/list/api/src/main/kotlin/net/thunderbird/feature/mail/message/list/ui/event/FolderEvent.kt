package net.thunderbird.feature.mail.message.list.ui.event

import net.thunderbird.feature.mail.message.list.ui.state.Folder

/**
 * Represents UI events related to folder actions, extending [MessageListEvent].
 * These events are typically triggered by user interactions within a specific folder view.
 *
 * @see MessageListEvent
 */
sealed interface FolderEvent : MessageListEvent {
    /**
     * Event to expunge (permanently delete) messages marked for deletion in a specific folder.
     *
     * @param folder The folder from which to expunge messages.
     */
    data class Expunge(val folder: Folder) : FolderEvent

    /**
     * Event to create a new folder to be used as the archive folder.
     *
     * @param name The name of the folder to be created.
     */
    data class CreateArchiveFolder(val name: String) : FolderEvent

    /**
     * Event to assign an existing folder as the archive folder.
     *
     * @param folder The folder to be designated as the archive folder.
     */
    data class AssignArchiveFolder(val folder: Folder) : FolderEvent

    /**
     * Event to mark all messages within a specific folder as read.
     *
     * @param folder The folder in which all messages should be marked as read.
     */
    data class MarkAllMessagesAsRead(val folder: Folder) : FolderEvent
}

/**
 * Represents events that are specific to the Trash folder.
 */
sealed interface TrashFolderEvent : FolderEvent {
    /**
     * Event to permanently delete all messages in the Trash folder.
     */
    data object EmptyTrash : FolderEvent
}

/**
 * Represents events that are specific to the Outbox folder.
 */
sealed interface OutboxFolderEvent : FolderEvent {
    /**
     * Event to trigger sending all pending messages in the Outbox.
     */
    data object SendPendingMessages : FolderEvent
}

/**
 * Represents events that are specific to the Spam folder.
 */
sealed interface SpamFolderEvent : FolderEvent {
    /**
     * Event to permanently delete all messages in the spam folder.
     */
    data object EmptySpamFolder : FolderEvent
}
