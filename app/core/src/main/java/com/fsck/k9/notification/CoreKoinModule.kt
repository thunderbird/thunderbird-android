package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.AccountPreferenceSerializer
import java.util.concurrent.Executors
import org.koin.dsl.module

val coreNotificationModule = module {
    single { NotificationController(get(), get(), get(), get(), get()) }
    single { NotificationManagerCompat.from(get()) }
    single { NotificationHelper(get(), get(), get()) }
    single {
        NotificationChannelManager(
            get(),
            Executors.newSingleThreadExecutor(),
            get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
            get()
        )
    }
    single {
        AccountPreferenceSerializer(
            storageManager = get(),
            resourceProvider = get(),
            serverSettingsSerializer = get()
        )
    }
    single { CertificateErrorNotificationController(get(), get(), get()) }
    single { AuthenticationErrorNotificationController(get(), get(), get()) }
    single { SyncNotificationController(get(), get(), get()) }
    single { SendFailedNotificationController(get(), get(), get()) }
    single { NewMailNotifications(get(), get(), get(), get()) }
    single { NotificationContentCreator(get(), get()) }
    single { SingleMessageNotificationCreator(get(), get(), get(), get()) }
    single { SummaryNotificationCreator(get(), get(), get(), get(), get()) }
    single { LockScreenNotificationCreator(get(), get()) }
    single {
        PushNotificationManager(
            context = get(),
            resourceProvider = get(),
            notificationChannelManager = get(),
            notificationManager = get()
        )
    }
}
