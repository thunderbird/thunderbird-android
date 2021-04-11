package com.fsck.k9.storage.messages

import com.fsck.k9.Account
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.MessageStore
import com.fsck.k9.mailstore.MessageStoreFactory
import com.fsck.k9.mailstore.StorageManager
import com.fsck.k9.message.extractors.BasicPartInfoExtractor

class K9MessageStoreFactory(
    private val localStoreProvider: LocalStoreProvider,
    private val storageManager: StorageManager,
    private val basicPartInfoExtractor: BasicPartInfoExtractor
) : MessageStoreFactory {
    override fun create(account: Account): MessageStore {
        val localStore = localStoreProvider.getInstance(account)
        return K9MessageStore(localStore, storageManager, basicPartInfoExtractor, account.uuid)
    }
}
