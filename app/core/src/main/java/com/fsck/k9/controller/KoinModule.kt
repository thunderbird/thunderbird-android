package com.fsck.k9.controller

import org.koin.dsl.module.applicationContext

val controllerModule = applicationContext {
    bean { MessagingController(get(), get(), get(), get(), get(), get(), get(), get(), get("controllerExtensions")) }
    bean { DefaultUnreadMessageCountProvider(get(), get(), get(), get()) as UnreadMessageCountProvider }
}
