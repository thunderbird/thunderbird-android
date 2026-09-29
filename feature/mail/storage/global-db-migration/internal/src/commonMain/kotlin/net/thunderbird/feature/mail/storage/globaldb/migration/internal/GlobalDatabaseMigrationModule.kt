package net.thunderbird.feature.mail.storage.globaldb.migration.internal

import net.thunderbird.feature.mail.storage.globaldb.migration.AccountMigrationStateRepository
import net.thunderbird.feature.mail.storage.globaldb.migration.AppMigrationStateRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val featureMailDatabaseGlobalDbMigrationModule: Module = module {
    single<AccountMigrationStateRepository> {
        DefaultAccountMigrationStateRepository(
            appMigrationStateRepository = get(),
        )
    }

    single<AppMigrationStateRepository> {
        DefaultAppMigrationStateRepository()
    }
}
