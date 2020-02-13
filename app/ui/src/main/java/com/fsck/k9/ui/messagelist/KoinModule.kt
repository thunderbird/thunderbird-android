package com.fsck.k9.ui.messagelist

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val messageListUiModule = module {
    viewModel { MessageListViewModel(get()) }
    factory { DefaultFolderProvider() }
    factory { MessageListExtractor(get(), get()) }
    factory { MessageListLoader(get(), get(), get(), get()) }
    factory { MessageListLiveDataFactory(get(), get(), get()) }
}
