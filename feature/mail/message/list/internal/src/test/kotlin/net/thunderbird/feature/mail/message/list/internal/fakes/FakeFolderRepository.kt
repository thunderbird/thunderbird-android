package net.thunderbird.feature.mail.message.list.internal.fakes

import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.RemoteFolderDetails
import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.RemoteFolder

class FakeFolderRepository(
    private val localFolders: Map<AccountId, List<Folder>>,
    private val remoteFolders: Map<AccountId, List<RemoteFolder>>,
) : FolderRepository {
    override suspend fun getFolder(
        accountId: AccountId,
        folderId: Long,
    ): Folder? = localFolders[accountId]?.find { it.id == folderId }

    override suspend fun getFolderDetails(
        accountId: AccountId,
        folderId: Long,
    ): FolderDetails? = error("Not implemented")

    override fun getRemoteFolders(accountId: AccountId): List<RemoteFolder> = remoteFolders[accountId].orEmpty()

    override fun getRemoteFolderDetails(accountId: AccountId): List<RemoteFolderDetails> = error("Not implemented")

    override fun getPushFoldersFlow(accountId: AccountId): Flow<List<RemoteFolder>> = error("Not implemented")

    override fun getPushFolders(accountId: AccountId): List<RemoteFolder> = error("Not implemented")

    override fun getFolderServerId(
        accountId: AccountId,
        folderId: Long,
    ): String? = error("Not implemented")

    override fun getFolderId(
        accountId: AccountId,
        folderServerId: String,
    ): Long? = error("Not implemented")

    override fun isFolderPresent(
        accountId: AccountId,
        folderId: Long,
    ): Boolean = error("Not implemented")

    override fun updateFolderDetails(
        accountId: AccountId,
        folderDetails: FolderDetails,
    ) = error("Not implemented")

    override fun setIncludeInUnifiedInbox(
        accountId: AccountId,
        folderId: Long,
        includeInUnifiedInbox: Boolean,
    ) = error("Not implemented")

    override fun setVisible(
        accountId: AccountId,
        folderId: Long,
        visible: Boolean,
    ) = error("Not implemented")

    override fun setSyncEnabled(
        accountId: AccountId,
        folderId: Long,
        enable: Boolean,
    ) = error("Not implemented")

    override fun setNotificationsEnabled(
        accountId: AccountId,
        folderId: Long,
        enable: Boolean,
    ) = error("Not implemented")

    override fun setPushDisabled(accountId: AccountId) = error("Not implemented")

    override fun hasPushEnabledFolder(accountId: AccountId): Boolean = error("Not implemented")

    override fun hasPushEnabledFolderFlow(accountId: AccountId): Flow<Boolean> = error("Not implemented")
}
