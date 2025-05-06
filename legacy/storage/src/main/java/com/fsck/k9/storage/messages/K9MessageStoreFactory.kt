package com.fsck.k9.storage.messages

import app.k9mail.legacy.account.LegacyAccount
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.NotifierMessageStore
import com.fsck.k9.mailstore.StorageFilesProviderFactory
import com.fsck.k9.message.extractors.BasicPartInfoExtractor

class K9MessageStoreFactory(
    private val localStoreProvider: LocalStoreProvider,
    private val storageFilesProviderFactory: StorageFilesProviderFactory,
    private val basicPartInfoExtractor: BasicPartInfoExtractor,
) : MessageStoreFactory {
    override fun create(account: LegacyAccount): ListenableMessageStore {
        val localStore = localStoreProvider.getInstance(account)
        val storageFilesProvider = storageFilesProviderFactory.createStorageFilesProvider(account.uuid)
        val messageStore = K9MessageStore(
            localStore.database,
            storageFilesProvider,
            basicPartInfoExtractor,
        )
        val notifierMessageStore = NotifierMessageStore(messageStore, localStore)
        return ListenableMessageStore(notifierMessageStore)
    }
}
