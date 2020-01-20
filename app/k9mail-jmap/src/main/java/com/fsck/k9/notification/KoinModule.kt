package com.fsck.k9.notification

import org.koin.dsl.module

val notificationModule = module {
    single<NotificationActionCreator> { K9NotificationActionCreator(get()) }
    single<NotificationResourceProvider> { K9NotificationResourceProvider(get()) }
    single<NotificationStrategy> { K9NotificationStrategy(get()) }
}
