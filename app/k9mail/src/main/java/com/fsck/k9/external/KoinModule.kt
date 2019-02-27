package com.fsck.k9.external

import org.koin.dsl.module.module

val externalModule = module {
    single { BroadcastSenderListener(get()) }
}
