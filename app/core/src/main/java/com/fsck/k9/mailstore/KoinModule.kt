package com.fsck.k9.mailstore

import org.koin.dsl.module.applicationContext

val mailStoreModule = applicationContext {
    bean { FolderRepositoryManager(get(), get()) }
    bean { MessageViewInfoExtractor(get(), get(), get()) }
    bean { StorageManager.getInstance(get()) }
    bean { SearchStatusManager() }
    bean { SpecialFolderSelectionStrategy() }
    bean { K9BackendStorageFactory(get(), get(), get()) }
}
