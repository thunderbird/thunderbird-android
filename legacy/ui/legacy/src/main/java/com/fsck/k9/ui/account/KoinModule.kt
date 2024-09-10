package com.fsck.k9.ui.account

import app.k9mail.legacy.ui.account.AccountFallbackImageProvider
import app.k9mail.legacy.ui.account.AccountImageLoader
import org.koin.dsl.module

val accountUiModule = module {
    factory { AccountImageLoader(accountFallbackImageProvider = get()) }
    factory { AccountFallbackImageProvider(context = get()) }
    factory { AccountImageModelLoaderFactory(contactPhotoLoader = get(), accountFallbackImageProvider = get()) }
}
