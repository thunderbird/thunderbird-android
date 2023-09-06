package app.k9mail.feature.account.server.config

import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigContract
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigValidator
import app.k9mail.feature.account.server.config.ui.incoming.AccountIncomingConfigViewModel
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigValidator
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountServerConfigModule: Module = module {
    factory<AccountIncomingConfigContract.Validator> { AccountIncomingConfigValidator() }
    factory<AccountOutgoingConfigContract.Validator> { AccountOutgoingConfigValidator() }

    viewModel {
        AccountIncomingConfigViewModel(
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel {
        AccountOutgoingConfigViewModel(
            validator = get(),
            accountStateRepository = get(),
        )
    }
}
