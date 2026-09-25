package app.k9mail.legacy.mailstore.domain

import app.k9mail.legacy.mailstore.MessageStoreManager

class SetPushForFolderUseCase(
    private val messageStoreManager: MessageStoreManager,
) {
    operator fun invoke(accountUuid: String, folderId: Long, enabled: Boolean) {
        messageStoreManager.getMessageStore(accountUuid).setPushEnabled(folderId, enabled)
    }
}
