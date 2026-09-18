package app.k9mail.legacy.mailstore.domain

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.mail.FolderType

class GetFolderIdsForTypeUseCase(
    private val messageStoreManager: MessageStoreManager,
) {
    operator fun invoke(
        accountUuid: String,
        folderType: FolderType,
    ): List<Long?> {
        return messageStoreManager.getMessageStore(accountUuid)
            .getFolders(true) { folderDetails ->
                if (folderDetails.type == folderType) {
                    folderDetails.id
                } else {
                    null
                }
            }
    }
}
