package com.fsck.k9.mailstore

import org.koin.dsl.module

val mailStoreModule = module {
    single { FolderRepositoryManager(get(), get()) }
    single { MessageViewInfoExtractorFactory(get(), get(), get()) }
    single { StorageManager.getInstance(get()) }
    single { SearchStatusManager() }
    single { SpecialFolderSelectionStrategy() }
    single { K9BackendStorageFactory(get(), get(), get(), get()) }
}
