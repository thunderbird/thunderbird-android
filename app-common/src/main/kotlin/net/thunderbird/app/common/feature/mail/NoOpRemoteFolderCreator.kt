package net.thunderbird.app.common.feature.mail

import com.fsck.k9.mail.FolderType
import com.fsck.k9.mail.folders.FolderServerId
import net.thunderbird.backend.api.folder.RemoteFolderCreationOutcome
import net.thunderbird.backend.api.folder.RemoteFolderCreator
import net.thunderbird.core.outcome.Outcome

/**
 * A [RemoteFolderCreator] that does nothing and always returns [RemoteFolderCreationOutcome.Success.AlreadyExists].
 */
object NoOpRemoteFolderCreator : RemoteFolderCreator {
    override suspend fun create(
        folderServerId: FolderServerId,
        mustCreate: Boolean,
        folderType: FolderType,
    ): Outcome<RemoteFolderCreationOutcome.Success, RemoteFolderCreationOutcome.Error> {
        return Outcome.success(RemoteFolderCreationOutcome.Success.AlreadyExists)
    }
}
