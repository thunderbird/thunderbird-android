package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val coreNotificationModule = module {
    single {
        NotificationController(
            certificateErrorNotificationController = get(),
            authenticationErrorNotificationController = get(),
            syncNotificationController = get(),
            sendFailedNotificationController = get(),
            newMailNotificationController = get(),
            logger = get(),
        )
    }
    single { NotificationManagerCompat.from(get()) }
    single {
        NotificationHelper(
            context = get(),
            notificationManager = get(),
            notificationChannelManager = get(),
            resourceProvider = get(),
            generalSettingsManager = get(),
            notificationIdRegistry = get(),
            accountManager = get(),
            logger = get(),
        )
    }
    single {
        NotificationChannelManager(
            accountManager = get(),
            backgroundExecutor = Executors.newSingleThreadExecutor(),
            notificationManager = get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
            resourceProvider = get(),
            notificationLightDecoder = get(),
        )
    }
    single {
        CertificateErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            generalSettingsManager = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    single {
        AuthenticationErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            generalSettingsManager = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    single {
        SyncNotificationController(
            notificationHelper = get(),
            actionBuilder = get(),
            resourceProvider = get(),
            outboxFolderManager = get(),
            iconResourceProvider = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    single {
        SendFailedNotificationController(
            notificationHelper = get(),
            actionBuilder = get(),
            resourceProvider = get(),
            generalSettingsManager = get(),
            outboxFolderManager = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    single {
        NewMailNotificationController(
            notificationManager = get(),
            newMailNotificationManager = get(),
            summaryNotificationCreator = get(),
            singleMessageNotificationCreator = get(),
        )
    }
    single {
        @OptIn(ExperimentalTime::class)
        NewMailNotificationManager(
            contentCreator = get(),
            notificationRepository = get(),
            baseNotificationDataCreator = get(),
            singleMessageNotificationDataCreator = get(),
            summaryNotificationDataCreator = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
            clock = get(),
        )
    }
    factory {
        NotificationContentCreator(
            resourceProvider = get(),
            contactRepository = get(),
            messageListPreferencesManager = get(),
        )
    }
    factory { BaseNotificationDataCreator() }
    factory {
        SingleMessageNotificationDataCreator(
            interactionPreferences = get(),
            notificationPreference = get(),
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    factory {
        SummaryNotificationDataCreator(
            singleMessageNotificationDataCreator = get(),
            generalSettingsManager = get(),
        )
    }
    factory {
        SingleMessageNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            lockScreenNotificationCreator = get(),
            notificationPreferenceManager = get(),
            application = androidApplication(),
        )
    }
    factory {
        SummaryNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            lockScreenNotificationCreator = get(),
            singleMessageNotificationCreator = get(),
            resourceProvider = get(),
        )
    }
    factory { LockScreenNotificationCreator(notificationHelper = get(), resourceProvider = get()) }
    single {
        PushNotificationManager(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
            notificationManager = get(),
            iconResourceProvider = get(),
            logger = get(),
        )
    }
    single {
        NotificationDataStore(
            accountManager = get(),
            notificationIdRegistry = get(),
        )
    }
    single {
        NotificationRepository(
            notificationStoreProvider = get(),
            localStoreProvider = get(),
            messageStoreManager = get(),
            notificationContentCreator = get(),
            generalSettingsManager = get(),
            notificationDataStore = get(),
            accountManager = get(),
        )
    }
    factory { NotificationLightDecoder() }
    factory { NotificationVibrationDecoder() }
    factory {
        NotificationConfigurationConverter(notificationLightDecoder = get(), notificationVibrationDecoder = get())
    }
    factory {
        NotificationSettingsUpdater(
            notificationChannelManager = get(),
            notificationConfigurationConverter = get(),
            accountManager = get(),
        )
    }
    factory<BackgroundWorkNotificationController> {
        RealBackgroundWorkNotificationController(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
        )
    }

    single<AccountNotificationIdRegistry> {
        DefaultAccountNotificationIdRegistry(
            accountManager = get(),
        )
    }
}
