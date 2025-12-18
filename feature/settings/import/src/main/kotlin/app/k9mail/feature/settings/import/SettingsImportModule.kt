package app.k9mail.feature.settings.import

import app.k9mail.feature.settings.import.ui.AuthViewModel
import app.k9mail.feature.settings.import.ui.ImportAppFetcher
import app.k9mail.feature.settings.import.ui.PickAppViewModel
import app.k9mail.feature.settings.import.ui.SettingsImportViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureSettingsImportModule = module {
    factory {
        ImportAppFetcher(
            context = androidContext(),
            logger = get(),
        )
    }

    viewModel {
        SettingsImportViewModel(
            contentResolver = get(),
            settingsImporter = get(),
            accountActivator = get(),
            migrationManager = get(),
            importAppFetcher = get(),
            logger = get(),
        )
    }

    viewModel {
        AuthViewModel(
            application = get(),
            accountManager = get(),
            getOAuthRequestIntent = get(),
            logger = get(),
        )
    }

    viewModel { PickAppViewModel(importAppFetcher = get()) }
}
