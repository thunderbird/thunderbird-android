package com.fsck.k9.controller.push

import org.koin.dsl.module

internal val controllerPushModule = module {
    single { PushServiceManager(context = get()) }
    single { BootCompleteManager(context = get()) }
    single { AutoSyncManager(context = get()) }
    single {
        AccountPushControllerFactory(
            backendManager = get(),
            messagingController = get(),
            folderRepository = get(),
            preferences = get()
        )
    }
    single {
        PushController(
            preferences = get(),
            generalSettingsManager = get(),
            backendManager = get(),
            pushServiceManager = get(),
            bootCompleteManager = get(),
            autoSyncManager = get(),
            pushNotificationManager = get(),
            connectivityManager = get(),
            accountPushControllerFactory = get()
        )
    }
}
