package com.fsck.k9.external

import org.koin.dsl.module.applicationContext

val externalModule = applicationContext {
    bean { BroadcastSenderListener(get()) }
}
