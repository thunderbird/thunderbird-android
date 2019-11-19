package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.fsck.k9.AccountPreferenceSerializer
import org.koin.dsl.module
import java.util.concurrent.Executors

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
    single { AccountPreferenceSerializer(get(), get()) }
    single { CertificateErrorNotifications(get(), get(), get()) }
    single { AuthenticationErrorNotifications(get(), get(), get()) }
    single { SyncNotifications(get(), get(), get()) }
    single { SendFailedNotifications(get(), get(), get()) }
    single { NewMailNotifications(get(), get(), get(), get()) }
    single { NotificationContentCreator(get(), get()) }
    single { WearNotifications(get(), get(), get()) }
    single { DeviceNotifications(get(), get(), get(), get(), get()) }
    single { LockScreenNotification(get(), get()) }
}
