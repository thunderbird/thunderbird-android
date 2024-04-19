package com.fsck.k9.preferences

import com.fsck.k9.Preferences
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val preferencesModule = module {
    factory {
        SettingsExporter(
            contentResolver = get(),
            preferences = get(),
            folderSettingsProvider = get(),
            folderRepository = get(),
            notificationSettingsUpdater = get(),
        )
    }
    factory { FolderSettingsProvider(folderRepository = get()) }
    factory<AccountManager> { get<Preferences>() }
    single {
        RealGeneralSettingsManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope")),
        )
    } bind GeneralSettingsManager::class

    factory { SettingsFileParser() }
    factory {
        SettingsImporter(
            settingsFileParser = get(),
            preferences = get(),
            generalSettingsManager = get(),
            localFoldersCreator = get(),
            serverSettingsSerializer = get(),
            clock = get(),
            context = get(),
        )
    }
}
