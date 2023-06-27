package com.fsck.k9.account

import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val newAccountModule = module {
    factory {
        AccountOwnerNameProvider(
            preferences = get(),
        )
    }

    factory {
        AccountCreator(
            accountCreatorHelper = get(),
            localFoldersCreator = get(),
            preferences = get(),
            context = androidApplication()
        )
    }

    factory {
        AccountSetupFinishedLauncher(
            context = androidContext(),
            preferences = get(),
        )
    }
}
