package com.fsck.k9.ui.messagesource

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val messageSourceModule = module {
    viewModel { MessageHeadersViewModel(messageRepository = get()) }
}
