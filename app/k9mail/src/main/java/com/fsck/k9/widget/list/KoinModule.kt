package com.fsck.k9.widget.list

import org.koin.dsl.module.applicationContext

val messageListWidgetModule = applicationContext {
    bean { MessageListWidgetUpdateListener(get()) }
}
