package net.thunderbird.feature.mail.message.list.internal.fakes

import app.k9mail.legacy.mailstore.FolderRepository
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.data.FolderError

class FakeFolderRepository(
    private val localFolders: Map<AccountId, List<Folder>> = emptyMap(),
) : FolderRepository {
    override suspend fun getFolder(
        accountId: AccountId,
        folderId: Long,
    ): Folder? = localFolders[accountId]?.find { it.id == folderId }

    override fun getFolderServerId(accountId: AccountId, folderId: Long): String? = error("Not implemented")

    override fun getFolderId(accountId: AccountId, folderServerId: String): Long? = error("Not implemented")

    override fun isFolderPresent(accountId: AccountId, folderId: Long): Boolean = error("Not implemented")

    override fun observeEnabled(accountId: AccountId): Flow<Outcome<Boolean, FolderError>> =
        error("Not implemented")

    override suspend fun isEnabled(accountId: AccountId): Outcome<Boolean, FolderError> = error("Not implemented")

    override suspend fun disable(accountId: AccountId): Outcome<Unit, FolderError> = error("Not implemented")
}
