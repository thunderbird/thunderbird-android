package com.fsck.k9.mailstore

import org.koin.dsl.module.applicationContext

val mailStoreModule = applicationContext {
    bean { FolderRepositoryManager() }
}
