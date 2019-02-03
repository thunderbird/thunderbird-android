package com.fsck.k9.notification

import org.koin.dsl.module.module

val notificationModule = module {
    single { K9NotificationActionCreator(get()) as NotificationActionCreator }
    single { K9NotificationResourceProvider(get()) as NotificationResourceProvider }
    single { K9NotificationStrategy(get()) as NotificationStrategy }
}
