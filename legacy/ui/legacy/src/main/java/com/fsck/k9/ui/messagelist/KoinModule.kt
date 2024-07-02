package com.fsck.k9.ui.messagelist

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val messageListUiModule = module {
    viewModel { MessageListViewModel(get()) }
    factory { DefaultFolderProvider() }
    factory {
        MessageListLoader(
            preferences = get(),
            localStoreProvider = get(),
            messageListRepository = get(),
            messageHelper = get(),
        )
    }
    factory {
        MessageListLiveDataFactory(messageListLoader = get(), preferences = get(), messageListRepository = get())
    }
    single { SortTypeToastProvider() }
}
