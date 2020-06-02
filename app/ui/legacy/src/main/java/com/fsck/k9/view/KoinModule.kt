package com.fsck.k9.view

import org.koin.dsl.module

val viewModule = module {
    single { WebViewConfigProvider(get()) }
}
