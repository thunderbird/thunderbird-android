package com.fsck.k9.ui.account

import net.thunderbird.core.ui.account.AccountFallbackImageProvider
import org.koin.dsl.module

val accountUiModule = module {
    factory { AccountFallbackImageProvider(context = get()) }
    factory { AccountImageModelLoaderFactory(contactPhotoLoader = get(), accountFallbackImageProvider = get()) }
}
