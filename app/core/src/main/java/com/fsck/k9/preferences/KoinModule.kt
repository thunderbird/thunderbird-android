package com.fsck.k9.preferences

import com.fsck.k9.Preferences
import org.koin.dsl.module

val preferencesModule = module {
    factory {
        SettingsExporter(
            contentResolver = get(),
            preferences = get(),
            folderSettingsProvider = get(),
            folderRepositoryManager = get()
        )
    }
    factory { FolderSettingsProvider(folderRepositoryManager = get()) }
    factory<AccountManager> { get<Preferences>() }
    single<GeneralSettingsManager> { RealGeneralSettingsManager(preferences = get()) }
}
