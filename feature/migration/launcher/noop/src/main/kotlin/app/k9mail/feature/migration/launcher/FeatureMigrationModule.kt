package app.k9mail.feature.migration.launcher

import app.k9mail.feature.migration.launcher.api.MigrationManager
import app.k9mail.feature.migration.launcher.noop.NoOpMigrationManager
import org.koin.dsl.module

val featureMigrationModule = module {
    single<MigrationManager> { NoOpMigrationManager() }
}
