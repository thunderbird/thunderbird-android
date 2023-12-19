package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOption
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.folders.FolderFetcher
import com.fsck.k9.mail.folders.RemoteFolder
import com.fsck.k9.mail.oauth.AuthStateStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GetSpecialFolderOptions(
    private val folderFetcher: FolderFetcher,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
    private val authStateStorage: AuthStateStorage,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : UseCase.GetSpecialFolderOptions {
    override suspend fun invoke(): SpecialFolderOptions {
        return withContext(coroutineDispatcher) {
            val serverSettings = accountStateRepository.getState().incomingServerSettings
                ?: error("No incoming server settings available")

            val remoteFolders = folderFetcher.getFolders(serverSettings, authStateStorage)
                .sortedWith(
                    compareByDescending<RemoteFolder> { it.type == FolderType.INBOX }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName },
                )

            SpecialFolderOptions(
                archiveSpecialFolderOptions = mapByFolderType(FolderType.ARCHIVE, remoteFolders),
                draftsSpecialFolderOptions = mapByFolderType(FolderType.DRAFTS, remoteFolders),
                sentSpecialFolderOptions = mapByFolderType(FolderType.SENT, remoteFolders),
                spamSpecialFolderOptions = mapByFolderType(FolderType.SPAM, remoteFolders),
                trashSpecialFolderOptions = mapByFolderType(FolderType.TRASH, remoteFolders),
            )
        }
    }

    private fun mapByFolderType(
        folderType: FolderType,
        remoteFolders: List<RemoteFolder>,
    ): List<SpecialFolderOption> {
        val automaticFolder = selectAutomaticFolderByType(folderType, remoteFolders)
        val folders = remoteFolders.map { remoteFolder ->
            getFolderByType(remoteFolder)
        }

        return (listOf(automaticFolder, SpecialFolderOption.None()) + folders)
    }

    // This uses the same implementation as the SpecialFolderSelectionStrategy. In case the implementation of the
    // SpecialFolderSelectionStrategy changes, this use case should be updated accordingly.
    private fun selectAutomaticFolderByType(
        folderType: FolderType,
        remoteFolders: List<RemoteFolder>,
    ): SpecialFolderOption = remoteFolders.firstOrNull { folder -> folder.type == folderType }
        ?.let {
            getFolderByType(
                remoteFolder = it,
                isAutomatic = true,
            )
        } ?: SpecialFolderOption.None(isAutomatic = true)

    private fun getFolderByType(
        remoteFolder: RemoteFolder,
        isAutomatic: Boolean = false,
    ): SpecialFolderOption {
        return when (remoteFolder.type) {
            FolderType.INBOX,
            FolderType.OUTBOX,
            FolderType.ARCHIVE,
            FolderType.DRAFTS,
            FolderType.SENT,
            FolderType.SPAM,
            FolderType.TRASH,
            -> SpecialFolderOption.Special(isAutomatic, remoteFolder)

            FolderType.REGULAR -> SpecialFolderOption.Regular(remoteFolder)
        }
    }
}
