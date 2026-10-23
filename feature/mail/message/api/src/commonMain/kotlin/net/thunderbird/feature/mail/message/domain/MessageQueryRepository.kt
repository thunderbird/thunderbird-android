package net.thunderbird.feature.mail.message.domain

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.FolderId
import net.thunderbird.feature.mail.message.MessageId
import net.thunderbird.feature.mail.message.MessageServerId

interface MessageQueryRepository {
    suspend fun findIdByCriteria(
        accountId: AccountId,
        criteria: GetMessageIdCriteria,
    ): Outcome<MessageId?, MessageQueryError>

    /**
     * Returns the server ids of the messages stored locally in a folder that are also known to the server.
     *
     * Local-only messages that haven't been uploaded yet (they carry a locally generated server id), messages
     * flagged as deleted and empty threading placeholders are not included.
     *
     * @param folderId The folder to list.
     * @param accountId The account the folder belongs to.
     * @return An [Outcome] with the set of server ids on success (empty if the folder has no messages), or a
     *   [MessageQueryError] on failure.
     */
    suspend fun getAllServerIdByFolderId(
        folderId: FolderId,
        accountId: AccountId,
    ): Outcome<Set<MessageServerId>, MessageQueryError>
}

sealed interface MessageQueryError {
    val throwable: Throwable?

    /**
     * The account the query was scoped to does not exist (anymore).
     */
    data class AccountNotFound(val accountId: AccountId) : MessageQueryError {
        override val throwable: Throwable? = null
    }

    /**
     * The underlying data source threw while running the query.
     */
    data class UnhandledError(override val throwable: Throwable) : MessageQueryError
}

data class GetMessageIdCriteria(val folderId: FolderId, val messageServerId: MessageServerId)
