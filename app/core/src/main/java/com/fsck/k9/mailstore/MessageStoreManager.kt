package com.fsck.k9.mailstore

import com.fsck.k9.Account
import com.fsck.k9.preferences.AccountManager
import java.util.concurrent.ConcurrentHashMap

class MessageStoreManager(accountManager: AccountManager, private val messageStoreFactory: MessageStoreFactory) {
    private val messageStores = ConcurrentHashMap<String, MessageStore>()

    init {
        accountManager.addAccountRemovedListener { account ->
            removeMessageStore(account.uuid)
        }
    }

    fun getMessageStore(account: Account): MessageStore {
        return messageStores.getOrPut(account.uuid) { messageStoreFactory.create(account) }
    }

    private fun removeMessageStore(accountUuid: String) {
        messageStores.remove(accountUuid)
    }
}
