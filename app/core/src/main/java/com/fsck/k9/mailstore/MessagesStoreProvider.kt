package com.fsck.k9.mailstore

import com.fsck.k9.Account
import java.util.concurrent.ConcurrentHashMap

class MessagesStoreProvider(private val messageStoreFactory: MessageStoreFactory) {
    private val messageStores = ConcurrentHashMap<String, MessageStore>()

    fun getMessageStore(account: Account): MessageStore {
        return messageStores.getOrPut(account.uuid) { messageStoreFactory.create(account) }
    }
}
