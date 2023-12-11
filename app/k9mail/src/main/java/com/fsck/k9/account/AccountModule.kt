package com.fsck.k9.account

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import com.fsck.k9.controller.command.AccountCommandFactory
import com.fsck.k9.controller.command.ConcreteAccountCommandFactory
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val newAccountModule = module {
    factory<AccountSetupExternalContract.AccountOwnerNameProvider> {
        AccountOwnerNameProvider(
            preferences = get(),
        )
    }

    factory {
        AccountColorPicker(
            accountManager = get(),
            resources = get(),
        )
    }

    single<AccountCommandFactory> {
        ConcreteAccountCommandFactory(
            accountManager = get(),
            backendManager = get(),
            clock = get(),
        )
    }

    factory<AccountSetupExternalContract.AccountCreator> {
        AccountCreator(
            accountColorPicker = get(),
            localFoldersCreator = get(),
            accountCommandFactory = get(),
            preferences = get(),
            context = androidApplication(),
        )
    }

    factory<AccountCommonExternalContract.AccountStateLoader> {
        AccountStateLoader(
            accountManager = get(),
        )
    }

    factory<AccountEditExternalContract.AccountServerSettingsUpdater> {
        AccountServerSettingsUpdater(
            accountManager = get(),
        )
    }
}
