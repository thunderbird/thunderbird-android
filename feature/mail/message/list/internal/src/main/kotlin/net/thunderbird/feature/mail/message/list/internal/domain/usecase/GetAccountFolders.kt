package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import app.k9mail.legacy.mailstore.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.message.list.domain.AccountFolderError
import net.thunderbird.feature.mail.message.list.domain.DomainContract

internal class GetAccountFolders(
    private val folderRepository: FolderRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.GetAccountFolders {
    override suspend fun invoke(accountId: AccountId): Outcome<List<RemoteFolder>, AccountFolderError> =
        withContext(ioDispatcher) {
            try {
                Outcome.success(
                    data = folderRepository
                        .getRemoteFolders(accountId)
                        .filter { it.type == FolderType.REGULAR || it.type == FolderType.ARCHIVE },
                )
            } catch (e: MessagingException) {
                Outcome.failure(error = AccountFolderError(exception = e))
            }
        }
}
