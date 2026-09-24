package net.thunderbird.feature.mail.message.domain

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.Message
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId

/**
 * Manages the lifecycle of locally stored messages, covering creation, update, relocation and deletion.
 */
interface MessageLifecycleRepository {
    /**
     * Creates and stores a new message locally.
     *
     * @param message The message to create. The message's id field will be ignored and a new id will be assigned.
     * @param accountId The account this message belongs to.
     * @param folderId Target folder, or `null` to store the message in the account's outbox folder.
     * @return An [Outcome] containing the assigned [MessageId] on success, or a [MessageLifecycleError] on failure.
     */
    suspend fun create(
        message: Message,
        accountId: AccountId,
        folderId: FolderId? = null,
    ): Outcome<MessageId, MessageLifecycleError>

    suspend fun update(
        message: Message,
        accountId: AccountId,
        folderId: FolderId,
    ): Outcome<MessageId, MessageLifecycleError>

    /**
     * Moves a locally stored message to another folder.
     *
     * The message is stored under a new id in the destination folder. The source entry is kept as a placeholder
     * so threading information survives until the remote side confirms the move.
     *
     * @param messageId The id of the message to move.
     * @param destinationFolderId The folder the message is moved to.
     * @param accountId The account both the message and the destination folder belong to.
     * @return An [Outcome] containing the [MessageId] of the message in the destination folder on success, or a
     *   [MessageLifecycleError] on failure. The returned id differs from [messageId].
     */
    suspend fun move(
        messageId: MessageId,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<MessageId, MessageLifecycleError>

    /**
     * Moves several locally stored messages to another folder. See [move] for the semantics of a single move.
     *
     * This operation is not atomic: messages are moved one by one. If a move fails, messages processed before the
     * failure stay in the destination folder and the returned [Outcome] is a failure without any mapping.
     *
     * @param messageIds The ids of the messages to move. An empty list results in an empty mapping.
     * @param destinationFolderId The folder the messages are moved to.
     * @param accountId The account the messages and the destination folder belong to.
     * @return An [Outcome] containing a mapping from each source [MessageId] to the [MessageId] of the message in
     *   the destination folder on success, or a [MessageLifecycleError] on failure.
     */
    suspend fun moveAll(
        messageIds: List<MessageId>,
        destinationFolderId: FolderId,
        accountId: AccountId,
    ): Outcome<Map<MessageId, MessageId>, MessageLifecycleError>

    suspend fun copy(messageId: MessageId, destinationFolderId: FolderId): Outcome<MessageId, MessageLifecycleError>
    suspend fun destroy(
        serverIds: List<MessageServerId>,
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Unit, MessageLifecycleError>
}

sealed interface MessageLifecycleError {
    val throwable: Throwable?

    data class MessageAlreadyExists(val messageId: MessageId) : MessageLifecycleError {
        override val throwable: Throwable? = null
    }

    data class UnhandledError(override val throwable: Throwable) : MessageLifecycleError
}
