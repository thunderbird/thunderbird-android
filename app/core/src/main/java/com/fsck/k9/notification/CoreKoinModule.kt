package com.fsck.k9.notification

import android.support.v4.app.NotificationManagerCompat
import org.koin.dsl.module.applicationContext

val coreNotificationModule = applicationContext {
    bean { NotificationController(get(), get(), get(), get(), get()) }
    bean { NotificationManagerCompat.from(get()) }
    bean { NotificationHelper(get(), get(), get()) }
    bean { NotificationChannelUtils(get(), get()) }
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
