package com.fsck.k9.account

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.settings.import.SettingsImportExternalContract
import net.thunderbird.feature.account.usecase.GetDefaultAccountId
import org.koin.dsl.module

val newAccountModule = module {
    factory<AccountSetupExternalContract.AccountOwnerNameProvider> {
        AccountOwnerNameProvider(
            accountManager = get(),
            getDefaultAccountId = get(),
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

    factory<SettingsImportExternalContract.AccountActivator> {
        AccountActivator(
            context = get(),
            accountManager = get(),
            messagingController = get(),
        )
    }

    factory<DeletePolicyProvider> { DefaultDeletePolicyProvider() }
}
