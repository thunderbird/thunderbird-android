package com.fsck.k9.activity.setup

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val setUpModule = module {
    viewModel { (accountId: String) ->
        AccountSetupCompositionViewModel(
            legacyAccountManager = get(),
            accountUuid = accountId,
            resources = get(),
            emailAddressValidator = get(),
        )
    }
}
