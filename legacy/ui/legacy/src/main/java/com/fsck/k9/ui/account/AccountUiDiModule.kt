package com.fsck.k9.ui.account

import net.thunderbird.core.ui.account.AccountFallbackImageProvider
import org.koin.dsl.module

internal val accountUiDiModule = module {
    factory { AccountFallbackImageProvider(context = get()) }
    factory { AccountImageModelLoaderFactory(contactPhotoLoader = get(), accountFallbackImageProvider = get()) }
}
