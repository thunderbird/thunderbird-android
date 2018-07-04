package com.fsck.k9.notification

import org.koin.dsl.module.applicationContext

val coreNotificationModule = applicationContext {
    bean { NotificationController.newInstance(get()) }
}
