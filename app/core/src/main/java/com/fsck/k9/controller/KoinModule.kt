package com.fsck.k9.controller

import org.koin.dsl.module.module

val controllerModule = module {
    single { MessagingController(get(), get(), get(), get(), get(), get(), get(), get(), get("controllerExtensions")) }
    single { DefaultAccountStatsCollector(get(), get(), get()) as AccountStatsCollector }
}
