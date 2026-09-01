package net.thunderbird.feature.mail.message.list.internal.fakes

import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository

class FakeRemoteFolderQueryRepository(
    private val remoteFolders: Map<AccountId, List<RemoteFolder>> = emptyMap(),
    private val exception: Exception? = null,
) : RemoteFolderQueryRepository {
    override suspend fun getAllByAccountId(accountId: AccountId): Outcome<List<RemoteFolder>, FolderError> =
        when (exception) {
            null -> Outcome.success(remoteFolders[accountId].orEmpty())

            is MessagingException -> Outcome.failure(
                FolderError.FailedToQueryDatabase(message = exception.message ?: "", throwable = exception),
            )

            else -> throw exception
        }
}
