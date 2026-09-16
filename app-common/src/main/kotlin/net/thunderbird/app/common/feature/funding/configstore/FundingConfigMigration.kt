package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult

class FundingConfigMigration: ConfigMigration {
    override suspend fun migrate(
        currentVersion: Int,
        newVersion: Int,
        current: Config,
    ): ConfigMigrationResult = ConfigMigrationResult.NoOp
}
