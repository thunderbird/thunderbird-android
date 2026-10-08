package net.thunderbird.feature.funding.googleplay.data.local.configstore

import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.feature.funding.googleplay.data.FundingDataContract.Local

/**
 * Migration for contribution configuration.
 *
 * Handles migration of contribution configuration between different versions.
 *
 * Please update the migration logic when necessary.
 */
internal class ContributionConfigMigration : Local.ContributionConfigMigration {
    override suspend fun migrate(
        currentVersion: Int,
        newVersion: Int,
        current: Config,
    ): ConfigMigrationResult = ConfigMigrationResult.NoOp
}
