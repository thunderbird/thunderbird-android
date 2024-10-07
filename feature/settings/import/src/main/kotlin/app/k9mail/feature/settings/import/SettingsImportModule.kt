package app.k9mail.feature.settings.import

import app.k9mail.feature.migration.qrcode.qrCodeModule
import app.k9mail.feature.settings.import.ui.AuthViewModel
import app.k9mail.feature.settings.import.ui.ImportAppFetcher
import app.k9mail.feature.settings.import.ui.PickAppViewModel
import app.k9mail.feature.settings.import.ui.SettingsImportViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val featureSettingsImportModule = module {
    includes(qrCodeModule)

    factory { ImportAppFetcher(context = get()) }

    viewModel {
        SettingsImportViewModel(
            contentResolver = get(),
            settingsImporter = get(),
            accountActivator = get(),
            importAppFetcher = get(),
        )
    }

    viewModel {
        AuthViewModel(
            application = get(),
            accountManager = get(),
            getOAuthRequestIntent = get(),
        )
    }

    viewModel { PickAppViewModel(importAppFetcher = get()) }
}
