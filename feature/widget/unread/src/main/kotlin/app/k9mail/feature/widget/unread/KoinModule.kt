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
            folderQueryRepository = get(),
            folderNameFormatter = get(),
            coreResourceProvider = get(),
            logger = get(),
        )
    }
    single {
        UnreadWidgetUpdater(
            context = get(),
            config = get(),
        )
    }
    single {
        UnreadWidgetUpdateListener(
            unreadWidgetUpdater = get(),
            logger = get(),
        )
    }
    single { UnreadWidgetMigrations(accountRepository = get(), folderQueryRepository = get()) }
}
