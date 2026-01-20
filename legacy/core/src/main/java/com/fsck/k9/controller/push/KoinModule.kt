package com.fsck.k9.controller.push

import org.koin.dsl.module

internal val controllerPushModule = module {
    single { PushServiceManager(context = get()) }
    single { BootCompleteManager(context = get()) }
    single { AutoSyncManager(context = get(), generalSettingsManager = get()) }
    single {
        AccountPushControllerFactory(
            accountManager = get(),
            backendManager = get(),
            messagingController = get(),
            folderRepository = get(),
            logger = get(),
        )
    }
    single {
        PushController(
            accountManager = get(),
            generalSettingsManager = get(),
            backendManager = get(),
            pushServiceManager = get(),
            bootCompleteManager = get(),
            autoSyncManager = get(),
            alarmPermissionManager = get(),
            pushNotificationManager = get(),
            connectivityManager = get(),
            accountPushControllerFactory = get(),
            folderRepository = get(),
        )
    }

    single<AlarmPermissionManager> { AlarmPermissionManager(context = get(), alarmManager = get()) }
}
