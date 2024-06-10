package app.k9mail.feature.widget.unread

import org.koin.dsl.module

val unreadWidgetModule = module {
    single {
        UnreadWidgetRepository(
            context = get(),
            dataRetriever = get(),
            migrations = get(),
        )
    }
    single {
        UnreadWidgetDataProvider(
            context = get(),
            preferences = get(),
            messageCountsProvider = get(),
            defaultFolderProvider = get(),
            folderRepository = get(),
            folderNameFormatter = get(),
        )
    }
    single {
        UnreadWidgetUpdater(
            context = get(),
            classProvider = get(),
        )
    }
    single { UnreadWidgetUpdateListener(unreadWidgetUpdater = get()) }
    single { UnreadWidgetMigrations(accountRepository = get(), folderRepository = get()) }
}
