package com.fsck.k9.account

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val newAccountModule = module {
    factory {
        AccountSetupFinishedLauncher(
            context = androidContext(),
            preferences = get(),
        )
    }
}
