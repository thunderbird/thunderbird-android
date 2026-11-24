package com.fsck.k9.notification

import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.message.controller.MessageReference
import net.thunderbird.feature.account.AccountId

internal class MessageStoreNewMessageStateStore(
    private val messageStoreManager: MessageStoreManager,
) : NewMessageStateStore {
    override fun setNewMessageState(accountId: AccountId, messageReference: MessageReference, newMessage: Boolean) {
        messageStoreManager.getMessageStore(accountId).setNewMessageState(
            folderId = messageReference.folderId,
            messageServerId = messageReference.uid,
            newMessage = newMessage,
        )
    }

    override fun clearNewMessageState(accountId: AccountId) {
        messageStoreManager.getMessageStore(accountId).clearNewMessageState()
    }
}
