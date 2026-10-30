package app.k9mail.legacy.mailstore.domain

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.mail.FolderType
import net.thunderbird.feature.account.AccountId

class GetFolderIdsForTypeUseCase(
    private val messageStoreManager: MessageStoreManager,
) {
    operator fun invoke(
        accountUuid: AccountId,
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
