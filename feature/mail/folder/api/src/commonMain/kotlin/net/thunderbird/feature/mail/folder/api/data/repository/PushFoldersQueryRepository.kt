package net.thunderbird.feature.mail.folder.api.data.repository

import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError

interface PushFoldersQueryRepository {
    /**
     * Returns a [Flow] of [RemoteFolder]s for the given [accountId] that should be used for push.
     *
     * @param accountId The account identifier.
     */
    fun observeAllByAccountId(accountId: AccountId): Flow<Outcome<List<RemoteFolder>, FolderError>>

    /**
     * Returns a list of [RemoteFolder]s for the given [accountId] that should be used for push.
     *
     * @param accountId The account identifier.
     */
    fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolder>, FolderError>
}
