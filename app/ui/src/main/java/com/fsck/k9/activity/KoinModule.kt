package com.fsck.k9.activity

import org.koin.dsl.module.applicationContext

val activityModule = applicationContext {
    bean { MessageLoaderHelperFactory(get(), get()) }
}
