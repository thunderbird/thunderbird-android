package com.fsck.k9.account

import org.koin.dsl.module.module

val accountModule = module {
    factory { AccountRemover(get(), get(), get()) }
    factory { BackgroundAccountRemover(get()) }
}
