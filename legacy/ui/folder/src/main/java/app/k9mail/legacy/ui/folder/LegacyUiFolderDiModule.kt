package app.k9mail.legacy.ui.folder

import org.koin.dsl.module

val legacyUiFolderDiModule = module {
    single<DisplayFolderRepository> {
        DefaultDisplayFolderRepository(
            accountManager = get(),
            messagingController = get(),
            messageStoreManager = get(),
            outboxFolderManager = get(),
        )
    }
}
