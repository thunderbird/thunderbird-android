package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMigration : ConfigMigration {
    override suspend fun migrate(
        currentVersion: Int,
        newVersion: Int,
        current: Config,
    ): ConfigMigrationResult {
        val updated = Config().apply {
            if (newVersion == 2) {
                resetFundingConfig()
            }
        }
        return ConfigMigrationResult.Migrated(
            updated = updated,
            keysToRemove = setOf(FundingConfigKeys.DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT),
        )
    }

    private fun resetFundingConfig(): Config {
        return Config().apply {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = 0L
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = 0
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN]
        }
    }
}
