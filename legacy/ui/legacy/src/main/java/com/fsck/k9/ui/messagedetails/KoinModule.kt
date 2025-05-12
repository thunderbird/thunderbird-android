package com.fsck.k9.ui.messagedetails

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageDetailsUiModule = module {
    viewModel {
        MessageDetailsViewModel(
            resources = get(),
            messageRepository = get(),
            folderRepository = get(),
            contactSettingsProvider = get(),
            contactRepository = get(),
            contactPermissionResolver = get(),
            clipboardManager = get(),
            accountManager = get(),
            participantFormatter = get(),
            folderNameFormatter = get(),
        )
    }
    factory { ContactSettingsProvider() }
    factory { AddToContactsLauncher() }
    factory { ShowContactLauncher() }
    factory { createMessageDetailsParticipantFormatter(contactNameProvider = get(), resources = get()) }
}
