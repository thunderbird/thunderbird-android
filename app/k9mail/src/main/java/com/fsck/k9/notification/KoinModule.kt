package com.fsck.k9.notification

import org.koin.dsl.module.applicationContext

val notificationModule = applicationContext {
    bean { K9NotificationActionCreator(get()) as NotificationActionCreator }
}
