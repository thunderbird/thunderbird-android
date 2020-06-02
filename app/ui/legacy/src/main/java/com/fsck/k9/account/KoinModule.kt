package com.fsck.k9.account

import org.koin.dsl.module

val accountModule = module {
    factory { AccountRemover(get(), get(), get()) }
    factory { BackgroundAccountRemover(get()) }
    factory { AccountCreator(get(), get()) }
}
