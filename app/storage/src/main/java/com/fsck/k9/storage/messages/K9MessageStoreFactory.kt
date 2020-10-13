package com.fsck.k9.storage.messages

import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.MessageStore
import com.fsck.k9.mailstore.MessageStoreFactory

class K9MessageStoreFactory(private val localStoreProvider: LocalStoreProvider) : MessageStoreFactory {
    override fun create(account: Account): MessageStore {
        val localStore = localStoreProvider.getInstance(account)
        return K9MessageStore(localStore)
    }
}
