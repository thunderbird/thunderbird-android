package net.thunderbird.feature.mail.folder.api.data.repository

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.RemoteFolderDetails
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface RemoteFolderDetailsRepository {
    /**
     * Returns a list of [FolderDetails] of a [RemoteFolder] for the given [accountId].
     *
     * @param accountId The account identifier.
     */
    suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolderDetails>, FolderError>
}
