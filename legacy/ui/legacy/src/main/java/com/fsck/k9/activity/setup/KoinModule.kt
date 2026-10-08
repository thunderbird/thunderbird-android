package com.fsck.k9.activity.setup

import com.fsck.k9.activity.account.identity.LegacyIdentitySignatureWebViewConfigurator
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val setUpModule = module {
    factory {
        LegacyIdentitySignatureWebViewConfigurator(
            webViewConfigProvider = get(),
            displayHtmlUiFactory = get(),
            htmlSignatureSanitizer = get(),
        )
    }
    viewModel { (accountId: String) ->
        AccountSetupCompositionViewModel(
            legacyAccountManager = get(),
            accountUuid = accountId,
            resources = get(),
            emailAddressValidator = get(),
            legacyIdentitySignatureWebViewConfigurator = get(),
        )
    }
}
