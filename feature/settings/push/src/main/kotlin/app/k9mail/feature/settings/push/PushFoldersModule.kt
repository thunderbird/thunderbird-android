package app.k9mail.feature.settings.push

import app.k9mail.feature.settings.push.ui.PushFoldersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val featureSettingsPushModule: Module = module {
    viewModel { (accountUuid: String) ->
        PushFoldersViewModel(
            accountUuid = accountUuid,
            accountManager = get(),
            alarmPermissionManager = get(),
        )
    }
}
