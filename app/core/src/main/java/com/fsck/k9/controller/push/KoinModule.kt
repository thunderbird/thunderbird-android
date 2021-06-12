package com.fsck.k9.controller.push

import org.koin.dsl.module

internal val controllerPushModule = module {
    single { PushServiceManager(context = get()) }
    single { PushController() }
}
