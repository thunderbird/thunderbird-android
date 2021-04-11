package com.fsck.k9.mailstore

import com.fsck.k9.message.extractors.AttachmentCounter
import com.fsck.k9.message.extractors.MessageFulltextCreator
import com.fsck.k9.message.extractors.MessagePreviewCreator
import org.koin.dsl.module

val mailStoreModule = module {
    single { FolderRepositoryManager(messageStoreManager = get()) }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single { StorageManager.getInstance(get()) }
    single { SearchStatusManager() }
    single { SpecialFolderSelectionStrategy() }
    single {
        K9BackendStorageFactory(
            preferences = get(),
            folderRepositoryManager = get(),
            localStoreProvider = get(),
            messageStoreManager = get(),
            specialFolderSelectionStrategy = get(),
            saveMessageDataCreator = get()
        )
    }
    factory { SpecialLocalFoldersCreator(preferences = get(), localStoreProvider = get()) }
    single { MessageStoreManager(accountManager = get(), messageStoreFactory = get()) }
    single { MessageRepository(messageStoreManager = get()) }
    factory { MessagePreviewCreator.newInstance() }
    factory { MessageFulltextCreator.newInstance() }
    factory { AttachmentCounter.newInstance() }
    factory {
        SaveMessageDataCreator(
            encryptionExtractor = get(),
            messagePreviewCreator = get(),
            messageFulltextCreator = get(),
            attachmentCounter = get()
        )
    }
}
