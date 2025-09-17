package net.thunderbird.app.common.feature.mail

import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.backend.imap.ImapRemoteFolderCreatorFactory
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.mail.account.api.BaseAccount

class FakeImapRemoteFolderCreatorFactory : ImapRemoteFolderCreatorFactory {

    var lastAccount: BaseAccount? = null

    override fun create(account: BaseAccount): RemoteFolderCreator {
        lastAccount = account

        return object : RemoteFolderCreator {
            override suspend fun create(
                folderServerId: com.fsck.k9.mail.folders.FolderServerId,
                mustCreate: Boolean,
                folderType: com.fsck.k9.mail.FolderType,
            ) = Outcome.success(RemoteFolderCreationOutcome.Success.AlreadyExists)
        }
    }
}
