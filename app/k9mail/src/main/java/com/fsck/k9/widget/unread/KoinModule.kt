package com.fsck.k9.widget.unread

import org.koin.dsl.module

val unreadWidgetModule = module {
    single { UnreadWidgetRepository(context = get(), dataRetriever = get(), migrations = get()) }
    single {
        UnreadWidgetDataProvider(
            context = get(),
            preferences = get(),
            messagingController = get(),
            defaultFolderProvider = get(),
            folderRepository = get(),
            folderNameFormatterFactory = get()
        )
    }
    single { UnreadWidgetUpdater(context = get()) }
    single { UnreadWidgetUpdateListener(unreadWidgetUpdater = get()) }
    single { UnreadWidgetMigrations(accountRepository = get(), folderRepository = get()) }
}
