package com.fsck.k9.storage.messages

import app.k9mail.legacy.mailstore.ListenableMessageStore
import app.k9mail.legacy.mailstore.MessageStoreFactory
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.mailstore.NotifierMessageStore
import com.fsck.k9.mailstore.StorageFilesProviderFactory
import com.fsck.k9.message.extractors.BasicPartInfoExtractor
import net.thunderbird.core.android.account.LegacyAccount

class K9MessageStoreFactory(
    private val localStoreProvider: LocalStoreProvider,
    private val storageFilesProviderFactory: StorageFilesProviderFactory,
    private val basicPartInfoExtractor: BasicPartInfoExtractor,
) : MessageStoreFactory {
    private lateinit var folderNameSanitizer: FolderNameSanitizer

    override fun create(account: LegacyAccount): ListenableMessageStore {
        val localStore = localStoreProvider.getInstance(account)
        if (account.incomingServerSettings.host.isGoogle() ||
            account.outgoingServerSettings.host.isGoogle()
        ) {
            if (!this::folderNameSanitizer.isInitialized) {
                folderNameSanitizer = FolderNameSanitizer(lockableDatabase = localStore.database)
            }
            folderNameSanitizer.removeGmailPrefixFromFolders()
        }
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

private fun String.isGoogle(): Boolean {
    val domains = listOf(".gmail.com", ".googlemail.com")
    return domains.any { this.endsWith(it, ignoreCase = true) }
}
