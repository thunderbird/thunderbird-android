package com.fsck.k9.notification

import android.support.v4.app.NotificationManagerCompat
import org.koin.dsl.module.applicationContext

val coreNotificationModule = applicationContext {
    bean { NotificationController(get(), get(), get(), get(), get()) }
    bean { NotificationManagerCompat.from(get()) }
    bean { NotificationHelper(get(), get()) }
    bean { CertificateErrorNotifications(get(), get()) }
    bean { AuthenticationErrorNotifications(get(), get()) }
    bean { SyncNotifications(get(), get()) }
    bean { SendFailedNotifications(get(), get()) }
    bean { NewMailNotifications(get(), get(), get(), get()) }
    bean { NotificationContentCreator(get()) }
    bean { WearNotifications(get(), get()) }
    bean { DeviceNotifications(get(), get(), get(), get()) }
    bean { LockScreenNotification(get()) }
}
