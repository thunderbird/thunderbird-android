package com.fsck.k9.ui.messagesource

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val messageSourceDiModule = module {
    viewModel { MessageHeadersViewModel(messageRepository = get()) }
}
