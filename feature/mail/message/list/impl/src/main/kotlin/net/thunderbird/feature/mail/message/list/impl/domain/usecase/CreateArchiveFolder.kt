package net.thunderbird.feature.mail.message.list.impl.domain.usecase

import com.fsck.k9.backend.api.FolderInfo
import com.fsck.k9.backend.api.createFolder
import com.fsck.k9.backend.api.updateFolders
import com.fsck.k9.mail.folders.FolderServerId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.thunderbird.backend.api.BackendStorageFactory
import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.account.api.AccountManager
import net.thunderbird.feature.mail.account.api.BaseAccount
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.SpecialFolderSelection
import net.thunderbird.feature.mail.folder.api.SpecialFolderUpdater
import net.thunderbird.feature.mail.message.list.api.domain.CreateArchiveFolderOutcome
import net.thunderbird.feature.mail.message.list.api.domain.DomainContract
import com.fsck.k9.mail.FolderType as LegacyFolderType

internal class CreateArchiveFolder(
    private val accountManager: AccountManager<BaseAccount>,
    private val backendStorageFactory: BackendStorageFactory<BaseAccount>,
    private val remoteFolderCreatorFactory: RemoteFolderCreator.Factory,
    private val specialFolderUpdaterFactory: SpecialFolderUpdater.Factory<BaseAccount>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DomainContract.UseCase.CreateArchiveFolder {
    override fun invoke(
        accountUuid: String,
        folderName: String,
    ): Flow<Outcome<CreateArchiveFolderOutcome.Success, CreateArchiveFolderOutcome.Error>> = flow {
        if (folderName.isBlank()) {
            emit(Outcome.failure(CreateArchiveFolderOutcome.Error.InvalidFolderName(folderName = folderName)))
            return@flow
        }

        val account = withContext(ioDispatcher) {
            accountManager.getAccount(accountUuid)
        } ?: run {
            emit(Outcome.failure(CreateArchiveFolderOutcome.Error.AccountNotFound))
            return@flow
        }

        val backendStorage = backendStorageFactory.createBackendStorage(account)
        val folderInfo = FolderInfo(
            serverId = folderName,
            name = folderName,
            type = LegacyFolderType.ARCHIVE,
        )

        try {
            val folderId = withContext(ioDispatcher) {
                backendStorage.updateFolders {
                    createFolder(folderInfo)
                }
            }

            if (folderId == null) {
                emit(
                    Outcome.failure(
                        CreateArchiveFolderOutcome.Error.LocalFolderCreationError(folderName = folderName),
                    ),
                )
            } else {
                emit(Outcome.success(CreateArchiveFolderOutcome.Success.LocalFolderCreated))
                val serverId = FolderServerId(folderInfo.serverId)
                emit(Outcome.success(CreateArchiveFolderOutcome.Success.SyncStarted(serverId = serverId)))
                val remoteFolderCreator = remoteFolderCreatorFactory.create(account)
                val outcome = remoteFolderCreator
                    .create(folderServerId = serverId, mustCreate = false, folderType = LegacyFolderType.ARCHIVE)
                when (outcome) {
                    is Outcome.Failure<RemoteFolderCreationOutcome.Error> -> emit(
                        Outcome.failure(
                            CreateArchiveFolderOutcome.Error.SyncError.Failed(
                                serverId = serverId,
                                message = outcome.error.toString(),
                                exception = null,
                            ),
                        ),
                    )

                    is Outcome.Success<RemoteFolderCreationOutcome.Success> -> handleRemoteFolderCreationSuccess(
                        localFolderId = folderId,
                        account = account,
                        emit = ::emit,
                    )
                }
            }
        } catch (e: MessagingException) {
            emit(Outcome.failure(CreateArchiveFolderOutcome.Error.UnhandledError(throwable = e)))
        }
    }

    private suspend fun handleRemoteFolderCreationSuccess(
        localFolderId: Long,
        account: BaseAccount,
        emit: suspend (Outcome<CreateArchiveFolderOutcome.Success, CreateArchiveFolderOutcome.Error>) -> Unit,
    ) {
        val specialFolderUpdater = specialFolderUpdaterFactory.create(account)
        emit(Outcome.success(CreateArchiveFolderOutcome.Success.UpdatingSpecialFolders))
        withContext(ioDispatcher) {
            specialFolderUpdater.setSpecialFolder(
                type = FolderType.ARCHIVE,
                folderId = localFolderId,
                selection = SpecialFolderSelection.MANUAL,
            )
            specialFolderUpdater.updateSpecialFolders()
            accountManager.saveAccount(account)
        }
        emit(Outcome.success(CreateArchiveFolderOutcome.Success.Created))
    }
}
