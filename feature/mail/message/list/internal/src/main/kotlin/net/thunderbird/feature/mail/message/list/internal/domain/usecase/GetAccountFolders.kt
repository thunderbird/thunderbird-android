package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.outcome.map
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.RemoteFolderQueryRepository
import net.thunderbird.feature.mail.message.list.domain.AccountFolderError
import net.thunderbird.feature.mail.message.list.domain.DomainContract

internal class GetAccountFolders(
    private val remoteFolderQueryRepository: RemoteFolderQueryRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.GetAccountFolders {
    override suspend fun invoke(accountId: AccountId): Outcome<List<RemoteFolder>, AccountFolderError> =
        withContext(ioDispatcher) {
            remoteFolderQueryRepository
                .getAllByAccountId(accountId)
                .map(
                    transformSuccess = { remoteFolders ->
                        remoteFolders.filter { it.type == FolderType.REGULAR || it.type == FolderType.ARCHIVE }
                    },
                    transformFailure = { error, _ ->
                        AccountFolderError(
                            exception = when (error) {
                                is FolderError.FailedToQueryDatabase -> error.throwable
                                else -> MessagingException("Failed to get account folders. Folder error = $error")
                            },
                        )
                    },
                )
        }
}
