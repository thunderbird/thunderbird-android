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

    suspend fun move(messageId: MessageId, destinationFolderId: FolderId): Outcome<MessageId, MessageLifecycleError>
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
