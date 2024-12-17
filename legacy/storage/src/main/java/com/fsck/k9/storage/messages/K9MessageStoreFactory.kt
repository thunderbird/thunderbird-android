package com.fsck.k9.storage.messages

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import com.fsck.k9.mailstore.DatabaseFilesProviderFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.NotifierMessageStore
import com.fsck.k9.message.extractors.BasicPartInfoExtractor

class K9MessageStoreFactory(
    private val localStoreProvider: LocalStoreProvider,
    private val databaseFilesProviderFactory: DatabaseFilesProviderFactory,
    private val basicPartInfoExtractor: BasicPartInfoExtractor,
) : MessageStoreFactory {
    override fun create(account: Account): ListenableMessageStore {
        val localStore = localStoreProvider.getInstance(account)
        val databaseFilesProvider = databaseFilesProviderFactory.createDatabaseFilesProvider(account.uuid)
        val messageStore = K9MessageStore(
            localStore.database,
            databaseFilesProvider,
            basicPartInfoExtractor,
        )
        val notifierMessageStore = NotifierMessageStore(messageStore, localStore)
        return ListenableMessageStore(notifierMessageStore)
    }
}
