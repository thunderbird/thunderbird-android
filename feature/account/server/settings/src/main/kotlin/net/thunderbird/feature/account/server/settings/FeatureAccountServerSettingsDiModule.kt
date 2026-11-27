package net.thunderbird.feature.account.server.settings

import app.k9mail.feature.account.common.domain.entity.InteractionMode
import net.thunderbird.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import net.thunderbird.feature.account.server.settings.ui.incoming.IncomingServerSettingsValidator
import net.thunderbird.feature.account.server.settings.ui.incoming.IncomingServerSettingsViewModel
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsValidator
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureAccountServerSettingsDiModule: Module = module {
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
