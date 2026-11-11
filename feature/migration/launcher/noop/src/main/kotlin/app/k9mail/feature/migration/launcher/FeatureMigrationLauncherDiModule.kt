package app.k9mail.feature.migration.launcher

import app.k9mail.feature.migration.launcher.api.MigrationManager
import app.k9mail.feature.migration.launcher.k9.K9MigrationManager
import app.k9mail.feature.migration.qrcode.featureMigrationQrCodeDiModule
import org.koin.dsl.module

val featureMigrationLauncherDiModule = module {
    includes(featureMigrationQrCodeDiModule)
    single<MigrationManager> { K9MigrationManager() }
}
