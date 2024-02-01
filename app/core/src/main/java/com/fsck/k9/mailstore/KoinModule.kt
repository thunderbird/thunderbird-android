package com.fsck.k9.mailstore

import com.fsck.k9.message.extractors.AttachmentCounter
import com.fsck.k9.message.extractors.MessageFulltextCreator
import com.fsck.k9.message.extractors.MessagePreviewCreator
import org.koin.dsl.module

val mailStoreModule = module {
    single { FolderRepository(messageStoreManager = get(), accountManager = get()) }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single { StorageManager.getInstance(get()) }
    single { SpecialFolderSelectionStrategy() }
    single {
        K9BackendStorageFactory(
            preferences = get(),
            folderRepository = get(),
            messageStoreManager = get(),
            specialFolderSelectionStrategy = get(),
            saveMessageDataCreator = get(),
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
            attachmentCounter = get(),
        )
    }
    single { MessageListRepository(messageStoreManager = get()) }
}
