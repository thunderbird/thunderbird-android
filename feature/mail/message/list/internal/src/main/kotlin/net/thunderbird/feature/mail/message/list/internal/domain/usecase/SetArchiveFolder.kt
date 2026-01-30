package net.thunderbird.feature.mail.message.list.internal.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.SetAccountFolderOutcome
import com.fsck.k9.mail.FolderType as LegacyFolderType

internal class SetArchiveFolder(
    private val accountManager: LegacyAccountManager,
    private val backendStorageFactory: BackendStorageFactory,
    private val specialFolderUpdaterFactory: SpecialFolderUpdater.Factory,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.SetArchiveFolder {
    override suspend fun invoke(
        accountId: AccountId,
        folder: RemoteFolder,
    ): Outcome<SetAccountFolderOutcome.Success, SetAccountFolderOutcome.Error> {
        val account = withContext(ioDispatcher) {
            accountManager.getById(accountId).firstOrNull()
        } ?: return Outcome.Failure(SetAccountFolderOutcome.Error.AccountNotFound)

        val backend = backendStorageFactory.createBackendStorage(accountId)
        val specialFolderUpdater = specialFolderUpdaterFactory.create(accountId)
        return try {
            withContext(ioDispatcher) {
                backend
                    .createFolderUpdater()
                    .use { updater ->
                        updater.changeFolder(
                            folderServerId = folder.serverId,
                            name = folder.name,
                            type = LegacyFolderType.ARCHIVE,
                        )
                        specialFolderUpdater.setSpecialFolder(
                            type = FolderType.ARCHIVE,
                            folderId = folder.id,
                            selection = SpecialFolderSelection.MANUAL,
                        )
                        specialFolderUpdater.updateSpecialFolders()
                        accountManager.saveAccount(account)

                        Outcome.success(SetAccountFolderOutcome.Success)
                    }
            }
        } catch (e: MessagingException) {
            Outcome.Failure(SetAccountFolderOutcome.Error.UnhandledError(throwable = e))
        }
    }
}
