package com.fsck.k9.widget.unread

import org.koin.dsl.module.applicationContext

val unreadWidgetModule = applicationContext {
    bean { UnreadWidgetRepository(get(), get()) }
    bean { UnreadWidgetDataProvider(get(), get(), get()) }
    bean { UnreadWidgetUpdater(get()) }
    bean { UnreadWidgetUpdateListener(get()) }
}
