package app.k9mail.feature.account.server.settings

import app.k9mail.feature.account.common.domain.entity.InteractionMode
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsValidator
import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsValidator
import app.k9mail.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureAccountServerSettingsModule: Module = module {
    factory<IncomingServerSettingsContract.Validator> { IncomingServerSettingsValidator() }
    factory<OutgoingServerSettingsContract.Validator> { OutgoingServerSettingsValidator() }

    viewModel {
        IncomingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = get(),
            accountStateRepository = get(),
        )
    }

    viewModel {
        OutgoingServerSettingsViewModel(
            mode = InteractionMode.Create,
            validator = get(),
            accountStateRepository = get(),
        )
    }
}
