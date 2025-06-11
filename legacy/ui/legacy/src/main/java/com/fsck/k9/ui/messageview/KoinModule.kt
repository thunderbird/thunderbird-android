package com.fsck.k9.ui.messageview

import org.koin.dsl.module

val messageViewUiModule = module {
    factory {
        createMessageViewRecipientFormatter(
            contactNameProvider = get(),
            resources = get(),
            generalSettingsManager = get(),
        )
    }
}
