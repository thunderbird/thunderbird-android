package com.fsck.k9.notification

import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationManagerCompat
import com.fsck.k9.AccountManager
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.mail.ssl.LocalKeyStore
import org.koin.dsl.module.applicationContext
import java.util.concurrent.Executors

val coreNotificationModule = applicationContext {
    bean { NotificationController(get(), get(), get(), get(), get()) }
    bean { NotificationManagerCompat.from(get()) }
    bean { NotificationHelper(get(), get(), get()) }
    bean {
        NotificationChannelManager(
                get(),
                Executors.newSingleThreadExecutor(),
                get<Context>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
                get()
        )
    }
    bean { AccountManager(get(), get()) }
    bean { LocalKeyStore.getInstance() }
    bean { LocalKeyStoreManager(get()) }
    bean { CertificateErrorNotifications(get(), get(), get()) }
    bean { AuthenticationErrorNotifications(get(), get(), get()) }
    bean { SyncNotifications(get(), get(), get()) }
    bean { SendFailedNotifications(get(), get(), get()) }
    bean { NewMailNotifications(get(), get(), get(), get()) }
    bean { NotificationContentCreator(get(), get()) }
    bean { WearNotifications(get(), get(), get()) }
    bean { DeviceNotifications(get(), get(), get(), get(), get()) }
    bean { LockScreenNotification(get(), get()) }
}
