package com.fsck.k9.ui.account

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val accountUiModule = module {
    viewModel {
        AccountsViewModel(accountManager = get(), messageCountsProvider = get(), messageListRepository = get())
    }
    factory { AccountImageLoader(accountFallbackImageProvider = get()) }
    factory { AccountFallbackImageProvider(context = get()) }
    factory { AccountImageModelLoaderFactory(contactPhotoLoader = get(), accountFallbackImageProvider = get()) }
}
