package com.fsck.k9.helper

import org.koin.dsl.module

val helperModule = module {
    single { ClipboardManager(get()) }
    single { MessageHelper.getInstance(get()) }
}
