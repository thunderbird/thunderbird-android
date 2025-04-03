package com.fsck.k9.preferences

import app.k9mail.legacy.account.AccountManager
import com.fsck.k9.Preferences
import net.thunderbird.core.preferences.DefaultSettingsChangeBroker
import net.thunderbird.core.preferences.GeneralSettingsManager
import net.thunderbird.core.preferences.SettingsChangeBroker
import net.thunderbird.core.preferences.SettingsChangePublisher
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

val preferencesModule = module {
    factory {
        SettingsExporter(
            contentResolver = get(),
            preferences = get(),
            folderSettingsProvider = get(),
            folderRepository = get(),
            notificationSettingsUpdater = get(),
            filePrefixProvider = get(),
        )
    }
    factory { FolderSettingsProvider(folderRepository = get()) }
    factory<AccountManager> { get<Preferences>() }
    single {
        RealGeneralSettingsManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope")),
            changePublisher = get(),
        )
    } bind GeneralSettingsManager::class
    single {
        RealDrawerConfigManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope")),
            changeBroker = get(),
        )
    } bind DrawerConfigManager::class

    factory { SettingsFileParser() }

    factory { GeneralSettingsValidator() }
    factory { GeneralSettingsUpgrader() }
    factory { GeneralSettingsWriter(preferences = get(), generalSettingsManager = get()) }

    factory { AccountSettingsValidator() }

    factory { IdentitySettingsUpgrader() }
    factory { FolderSettingsUpgrader() }
    factory { ServerSettingsUpgrader() }

    factory {
        AccountSettingsUpgrader(
            identitySettingsUpgrader = get(),
            folderSettingsUpgrader = get(),
            serverSettingsUpgrader = get(),
        )
    }

    factory {
        AccountSettingsWriter(
            preferences = get(),
            localFoldersCreator = get(),
            clock = get(),
            serverSettingsSerializer = get(),
            context = get(),
        )
    }

    factory { UnifiedInboxConfigurator(accountManager = get()) }

    factory {
        SettingsImporter(
            settingsFileParser = get(),
            generalSettingsValidator = get(),
            accountSettingsValidator = get(),
            generalSettingsUpgrader = get(),
            accountSettingsUpgrader = get(),
            generalSettingsWriter = get(),
            accountSettingsWriter = get(),
            unifiedInboxConfigurator = get(),
        )
    }

    single { DefaultSettingsChangeBroker() }
        .binds(
            arrayOf(
                SettingsChangePublisher::class,
                SettingsChangeBroker::class,
            ),
        )
}
