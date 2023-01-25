package com.fsck.k9.ui.messagedetails

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val messageDetailsUiModule = module {
    viewModel {
        MessageDetailsViewModel(
            resources = get(),
            messageRepository = get(),
            contactSettingsProvider = get(),
            contacts = get(),
            clipboardManager = get()
        )
    }
    factory { ContactSettingsProvider() }
    factory { AddToContactsLauncher() }
    factory { ShowContactLauncher() }
}
