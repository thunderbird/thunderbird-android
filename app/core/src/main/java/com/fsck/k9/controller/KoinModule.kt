package com.fsck.k9.controller

import android.content.Context
import com.fsck.k9.CoreResourceProvider
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.notification.NotificationController
import com.fsck.k9.notification.NotificationStrategy
import org.koin.core.qualifier.named
import org.koin.dsl.module

val controllerModule = module {
    single {
        MessagingController(
            get<Context>(),
            get<NotificationController>(),
            get<NotificationStrategy>(),
            get<LocalStoreProvider>(),
            get<UnreadMessageCountProvider>(),
            get<CoreResourceProvider>(),
            get<BackendManager>(),
            get<Preferences>(),
            get(named("controllerExtensions"))
        )
    }
    single<UnreadMessageCountProvider> { DefaultUnreadMessageCountProvider(get(), get(), get(), get()) }
}
