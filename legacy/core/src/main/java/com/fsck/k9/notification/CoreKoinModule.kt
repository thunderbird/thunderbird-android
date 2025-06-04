package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.AccountPreferenceSerializer
import java.util.concurrent.Executors
import org.koin.dsl.module

val coreNotificationModule = module {
    single {
        NotificationController(
            certificateErrorNotificationController = get(),
            authenticationErrorNotificationController = get(),
            syncNotificationController = get(),
            sendFailedNotificationController = get(),
            newMailNotificationController = get(),
        )
    }
    single { NotificationManagerCompat.from(get()) }
    single {
        NotificationHelper(
            context = get(),
            notificationManager = get(),
            notificationChannelManager = get(),
            resourceProvider = get(),
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
        AccountPreferenceSerializer(
            serverSettingsSerializer = get(),
        )
    }
    single {
        CertificateErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
        )
    }
    single {
        AuthenticationErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
        )
    }
    single {
        SyncNotificationController(notificationHelper = get(), actionBuilder = get(), resourceProvider = get())
    }
    single {
        SendFailedNotificationController(notificationHelper = get(), actionBuilder = get(), resourceProvider = get())
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
        NewMailNotificationManager(
            contentCreator = get(),
            notificationRepository = get(),
            baseNotificationDataCreator = get(),
            singleMessageNotificationDataCreator = get(),
            summaryNotificationDataCreator = get(),
            clock = get(),
        )
    }
    factory {
        NotificationContentCreator(
            resourceProvider = get(),
            contactRepository = get(),
            generalSettingsManager = get(),
        )
    }
    factory { BaseNotificationDataCreator() }
    factory { SingleMessageNotificationDataCreator() }
    factory { SummaryNotificationDataCreator(singleMessageNotificationDataCreator = get()) }
    factory {
        SingleMessageNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            lockScreenNotificationCreator = get(),
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
        )
    }
    single {
        NotificationRepository(
            notificationStoreProvider = get(),
            localStoreProvider = get(),
            messageStoreManager = get(),
            notificationContentCreator = get(),
        )
    }
    factory { NotificationLightDecoder() }
    factory { NotificationVibrationDecoder() }
    factory {
        NotificationConfigurationConverter(notificationLightDecoder = get(), notificationVibrationDecoder = get())
    }
    factory {
        NotificationSettingsUpdater(
            preferences = get(),
            notificationChannelManager = get(),
            notificationConfigurationConverter = get(),
        )
    }
    factory<BackgroundWorkNotificationController> {
        RealBackgroundWorkNotificationController(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
        )
    }
}
