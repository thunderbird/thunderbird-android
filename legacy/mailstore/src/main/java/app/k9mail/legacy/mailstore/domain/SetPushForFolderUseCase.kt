package app.k9mail.legacy.mailstore.domain

import app.k9mail.legacy.mailstore.MessageStoreManager
import net.thunderbird.feature.account.AccountId

class SetPushForFolderUseCase(
    private val messageStoreManager: MessageStoreManager,
) {
    operator fun invoke(accountUuid: AccountId, folderId: Long, enabled: Boolean) {
        messageStoreManager.getMessageStore(accountUuid).setPushEnabled(folderId, enabled)
    }
}
