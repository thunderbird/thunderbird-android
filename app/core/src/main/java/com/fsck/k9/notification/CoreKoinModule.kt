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
            newMailNotificationController = get()
        )
    }
    single { NotificationManagerCompat.from(get()) }
    single { NotificationHelper(context = get(), notificationManager = get(), channelUtils = get()) }
    single {
        NotificationChannelManager(
            preferences = get(),
            backgroundExecutor = Executors.newSingleThreadExecutor(),
            notificationManager = get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
            resourceProvider = get()
        )
    }
    single {
        AccountPreferenceSerializer(
            storageManager = get(),
            resourceProvider = get(),
            serverSettingsSerializer = get()
        )
    }
    single {
        CertificateErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get()
        )
    }
    single {
        AuthenticationErrorNotificationController(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get()
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
            notificationHelper = get(),
            contentCreator = get(),
            summaryNotificationCreator = get(),
            singleMessageNotificationCreator = get()
        )
    }
    single { NotificationContentCreator(context = get(), resourceProvider = get()) }
    single {
        SingleMessageNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            resourceProvider = get(),
            lockScreenNotificationCreator = get()
        )
    }
    single {
        SummaryNotificationCreator(
            notificationHelper = get(),
            actionCreator = get(),
            lockScreenNotificationCreator = get(),
            singleMessageNotificationCreator = get(),
            resourceProvider = get()
        )
    }
    single { LockScreenNotificationCreator(notificationHelper = get(), resourceProvider = get()) }
    single {
        PushNotificationManager(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
            notificationManager = get()
        )
    }
}
