package net.thunderbird.app.common.feature.mail

import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId

class FakeImapRemoteFolderCreatorFactory : ImapRemoteFolderCreatorFactory {

    var lastAccountId: AccountId? = null

    override suspend fun create(accountId: AccountId): RemoteFolderCreator {
        lastAccountId = accountId

        return object : RemoteFolderCreator {
            override suspend fun create(
                folderServerId: com.fsck.k9.mail.folders.FolderServerId,
                mustCreate: Boolean,
                folderType: com.fsck.k9.mail.FolderType,
            ) = Outcome.success(RemoteFolderCreationOutcome.Success.AlreadyExists)
        }
    }
}
