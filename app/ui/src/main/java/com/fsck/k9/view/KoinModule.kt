package com.fsck.k9.view

import org.koin.dsl.module.applicationContext

val viewModule = applicationContext {
    bean { WebViewConfigProvider(get()) }
}
