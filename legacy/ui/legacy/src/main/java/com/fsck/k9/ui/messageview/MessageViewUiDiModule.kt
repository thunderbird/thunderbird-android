package com.fsck.k9.ui.messageview

import org.koin.dsl.module

val messageViewUiDiModule = module {
    single { LastSelectedFolderStore() }
    factory {
        createMessageViewRecipientFormatter(
            contactNameProvider = get(),
            resources = get(),
            messageListPreferencesManager = get(),
        )
    }
}
