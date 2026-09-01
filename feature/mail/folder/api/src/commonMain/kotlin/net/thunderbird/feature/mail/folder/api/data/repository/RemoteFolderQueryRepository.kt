package net.thunderbird.feature.mail.folder.api.data.repository

import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface RemoteFolderQueryRepository {
    /**
     * Returns a list of [RemoteFolder]s for the given [accountId].
     *
     * @param accountId The account identifier.
     * @throws MessagingException if there's a problem accessing the folders.
     */
    suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolder>, FolderError>
}
