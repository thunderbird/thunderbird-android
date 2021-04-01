package com.fsck.k9.mailstore

import org.koin.dsl.module

val mailStoreModule = module {
    single { FolderRepositoryManager(localStoreProvider = get(), messageStoreManager = get()) }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single { StorageManager.getInstance(get()) }
    single { SearchStatusManager() }
    single { SpecialFolderSelectionStrategy() }
    single { K9BackendStorageFactory(get(), get(), get(), get()) }
    factory { SpecialLocalFoldersCreator(preferences = get(), localStoreProvider = get()) }
    single { MessageStoreManager(accountManager = get(), messageStoreFactory = get()) }
    single { MessageRepository(messageStoreManager = get()) }
}
