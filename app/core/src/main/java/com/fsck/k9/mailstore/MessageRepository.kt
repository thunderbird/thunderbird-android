package com.fsck.k9.mailstore

import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Header

class MessageRepository(private val messageStoreManager: MessageStoreManager) {
    fun getHeaders(messageReference: MessageReference): List<Header> {
        val messageStore = messageStoreManager.getMessageStore(messageReference.accountUuid)
        return messageStore.getHeaders(messageReference.folderId, messageReference.uid)
    }
}
