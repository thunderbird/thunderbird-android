package net.thunderbird.app.common.feature.funding.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMigration
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMigration : ConfigMigration {
    override suspend fun migrate(
        currentVersion: Int,
        newVersion: Int,
        current: Config,
    ): ConfigMigrationResult {
        // Only update if config version is updating to anything but first launch (version 1)
        return if (currentVersion != newVersion && newVersion != 1) {
            val keysToRemove = mutableSetOf<ConfigKey<*>>()
            val updated = Config().apply {
                if (currentVersion <= 1 && newVersion == 2) {
                    resetFundingConfig()
                    keysToRemove.add(FundingConfigKeys.DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT)
                }
            }
            ConfigMigrationResult.Migrated(
                updated = updated,
                keysToRemove = keysToRemove,
            )
        } else {
            ConfigMigrationResult.NoOp
        }
    }

    private fun resetFundingConfig(): Config {
        return Config().apply {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = 0L
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = 0
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN] = 0L
        }
    }
}
