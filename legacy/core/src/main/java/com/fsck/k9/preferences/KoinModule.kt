package com.fsck.k9.preferences

import com.fsck.k9.Preferences
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.logging.config.DebugLogConfigurator
import net.thunderbird.core.logging.config.PlatformInitializer
import net.thunderbird.core.preference.DefaultPreferenceChangeBroker
import net.thunderbird.core.preference.FolderListPreferenceMigration
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangePublisher
import net.thunderbird.core.preference.debugging.DebuggingSettingsPreferenceManager
import net.thunderbird.core.preference.debugging.DefaultDebuggingSettingsPreferenceManager
import net.thunderbird.core.preference.display.DefaultDisplaySettingsPreferenceManager
import net.thunderbird.core.preference.display.DisplaySettingsPreferenceManager
import net.thunderbird.core.preference.display.coreSettings.DefaultDisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.display.inboxSettings.DefaultDisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.miscSettings.DefaultDisplayMiscSettingsPreferenceManager
import net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DefaultDisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.message.list.DefaultMessageListPreferencesManager
import net.thunderbird.core.preference.display.visualSettings.message.list.MessageListPreferencesManager
import net.thunderbird.core.preference.interaction.DefaultInteractionSettingsPreferenceManager
import net.thunderbird.core.preference.interaction.InteractionSettingsPreferenceManager
import net.thunderbird.core.preference.network.DefaultNetworkSettingsPreferenceManager
import net.thunderbird.core.preference.network.NetworkSettingsPreferenceManager
import net.thunderbird.core.preference.notification.DefaultNotificationPreferenceManager
import net.thunderbird.core.preference.notification.NotificationPreferenceManager
import net.thunderbird.core.preference.privacy.DefaultPrivacySettingsPreferenceManager
import net.thunderbird.core.preference.privacy.PrivacySettingsPreferenceManager
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
    factory<LegacyAccountDtoManager> { get<Preferences>() }
    single<PrivacySettingsPreferenceManager> {
        DefaultPrivacySettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<NotificationPreferenceManager> {
        DefaultNotificationPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<DisplayCoreSettingsPreferenceManager> {
        DefaultDisplayCoreSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<DisplayInboxSettingsPreferenceManager> {
        DefaultDisplayInboxSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<DisplayVisualSettingsPreferenceManager> {
        DefaultDisplayVisualSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
            messageListPreferences = get(),
            preferenceChangeBroker = get(),
        )
    }
    single<DisplayMiscSettingsPreferenceManager> {
        DefaultDisplayMiscSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<InteractionSettingsPreferenceManager> {
        DefaultInteractionSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<DisplaySettingsPreferenceManager> {
        DefaultDisplaySettingsPreferenceManager(
            logger = get(),
            coreSettingsPreferenceManager = get(),
            inboxSettingsPreferenceManager = get(),
            visualSettingsPreferenceManager = get(),
            miscSettingsPreferenceManager = get(),
        )
    }
    single<MessageListPreferencesManager> {
        DefaultMessageListPreferencesManager(
            logger = get(),
            storage = get(),
            storageEditor = get(),
        )
    }
    single<NetworkSettingsPreferenceManager> {
        DefaultNetworkSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
        )
    }
    single<DebuggingSettingsPreferenceManager> {
        DefaultDebuggingSettingsPreferenceManager(
            logger = get(),
            storagePersister = get(),
            storageEditor = get<Preferences>().createStorageEditor(),
            logLevelManager = get(),
            platformConfigProvider = get(),
        )
    }
    single<PlatformInitializer> {
        PlatformInitializer()
    }
    single<DebugLogConfigurator> {
        DebugLogConfigurator(
            syncDebugCompositeSink = get(named("syncDebug")),
            syncDebugFileLogSink = get(named("syncDebug")),
            platformInitializer = get(),
        )
    }
    single {
        DefaultGeneralSettingsManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope")),
            changePublisher = get(),
            privacySettingsPreferenceManager = get(),
            notificationPreferenceManager = get(),
            displaySettingsSettingsPreferenceManager = get(),
            displayCoreSettingsPreferenceManager = get(),
            displayInboxSettingsPreferenceManager = get(),
            displayVisualSettingsPreferenceManager = get(),
            displayMiscSettingsPreferenceManager = get(),
            networkSettingsPreferenceManager = get(),
            debuggingSettingsPreferenceManager = get(),
            interactionSettingsPreferenceManager = get(),
            debugLogConfigurator = get(),
            platformConfigProvider = get(),
        )
    } bind GeneralSettingsManager::class
    single {
        DefaultDrawerConfigManager(
            preferences = get(),
            coroutineScope = get(named("AppCoroutineScope")),
            displayInboxSettingsPreferenceManager = get(),
            displayVisualSettingsPreferenceManager = get(),
        )
    } bind DrawerConfigManager::class

    factory { SettingsFileParser() }

    factory { GeneralSettingsValidator() }
    factory { GeneralSettingsUpgrader() }
    factory { GeneralSettingsWriter(preferences = get(), generalSettingsManager = get()) }

    factory { AccountSettingsValidator() }

    factory { IdentitySettingsUpgrader(generalSettingsManager = get()) }
    factory { FolderSettingsUpgrader(generalSettingsManager = get()) }
    factory { ServerSettingsUpgrader(generalSettingsManager = get()) }

    factory {
        AccountSettingsUpgrader(
            identitySettingsUpgrader = get(),
            folderSettingsUpgrader = get(),
            serverSettingsUpgrader = get(),
            generalSettingsManager = get(),
        )
    }

    factory {
        @OptIn(ExperimentalTime::class)
        AccountSettingsWriter(
            preferences = get(),
            localFoldersCreator = get(),
            clock = get(),
            serverSettingsDtoSerializer = get(),
            context = get(),
            generalSettingsManager = get(),
        )
    }

    factory { UnifiedInboxConfigurator(accountManager = get(), generalSettingsManager = get()) }

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

    single { FolderListPreferenceMigration(storage = get(), editor = get()) }
    single { DefaultPreferenceChangeBroker() }
        .binds(
            arrayOf(
                PreferenceChangePublisher::class,
                PreferenceChangeBroker::class,
            ),
        )
}
