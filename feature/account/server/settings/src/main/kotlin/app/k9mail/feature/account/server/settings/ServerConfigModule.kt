package app.k9mail.feature.account.server.settings

import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsValidator
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.outgoing.AccountOutgoingConfigContract
import app.k9mail.feature.account.server.settings.ui.outgoing.AccountOutgoingConfigValidator
import app.k9mail.feature.account.server.settings.ui.outgoing.AccountOutgoingConfigViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountServerConfigModule: Module = module {
    factory<IncomingServerSettingsContract.Validator> { IncomingServerSettingsValidator() }
    factory<AccountOutgoingConfigContract.Validator> { AccountOutgoingConfigValidator() }

    viewModel {
        IncomingServerSettingsViewModel(
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
