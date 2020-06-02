package com.fsck.k9.preferences

import org.koin.dsl.module

val preferencesModule = module {
    factory {
        SettingsExporter(
            contentResolver = get(),
            backendManager = get(),
            preferences = get(),
            folderSettingsProvider = get(),
            folderRepositoryManager = get()
        )
    }
    factory { FolderSettingsProvider(folderRepositoryManager = get()) }
}
