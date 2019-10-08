package com.fsck.k9.helper

import org.koin.dsl.module.applicationContext

val helperModule = applicationContext {
    bean { ClipboardManager(get()) }
    bean { MessageHelper.getInstance(get()) }
}
