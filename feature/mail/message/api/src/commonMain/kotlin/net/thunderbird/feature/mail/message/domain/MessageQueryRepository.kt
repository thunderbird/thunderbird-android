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
}

sealed interface MessageQueryError {
    val throwable: Throwable
}

data class GetMessageIdCriteria(val folderId: FolderId, val messageServerId: MessageServerId)
