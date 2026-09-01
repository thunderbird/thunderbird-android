package net.thunderbird.feature.mail.message.list.internal.fakes

import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository

class FakeFolderQueryRepository(
    private val localFolders: Map<AccountId, List<Folder>> = emptyMap(),
) : FolderQueryRepository {
    override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<Folder?, FolderError> =
        Outcome.success(localFolders[accountId]?.find { it.id == folderId })

    override suspend fun findFolderServerIdById(
        accountId: AccountId,
        folderId: Long,
    ): Outcome<FolderServerId?, FolderError> = error("Not implemented")

    override suspend fun findIdByServerId(
        accountId: AccountId,
        folderServerId: FolderServerId,
    ): Outcome<Long?, FolderError> = error("Not implemented")

    override suspend fun isPresent(accountId: AccountId, folderId: Long): Boolean = error("Not implemented")
}
