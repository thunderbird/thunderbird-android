package app.k9mail.feature.settings.import

import app.k9mail.feature.settings.import.ui.AuthViewModel
import app.k9mail.feature.settings.import.ui.SettingsImportViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureSettingsImportModule = module {
    viewModel {
        SettingsImportViewModel(
            context = get(),
            settingsImporter = get(),
            accountActivator = get(),
        )
    }

    viewModel {
        AuthViewModel(
            application = get(),
            accountManager = get(),
            getOAuthRequestIntent = get(),
        )
    }
}
