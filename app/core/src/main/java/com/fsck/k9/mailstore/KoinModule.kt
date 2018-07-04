package com.fsck.k9.mailstore

import org.koin.dsl.module.applicationContext

val mailStoreModule = applicationContext {
    bean { FolderRepositoryManager() }
    bean { MessageViewInfoExtractor(get(), get(), get()) }
    bean { StorageManager.getInstance(get()) }
    bean { SearchStatusManager() }
}
