package app.k9mail.feature.migration.launcher

import app.k9mail.feature.migration.launcher.api.MigrationManager
import app.k9mail.feature.migration.launcher.thunderbird.TbMigrationManager
import app.k9mail.feature.migration.qrcode.qrCodeModule
import org.koin.dsl.module

val featureMigrationModule = module {
    includes(qrCodeModule)

    single<MigrationManager> { TbMigrationManager() }
}
