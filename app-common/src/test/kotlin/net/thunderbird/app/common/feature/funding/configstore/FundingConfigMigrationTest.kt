package net.thunderbird.app.common.feature.funding.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMigrationResult
import net.thunderbird.feature.funding.api.FundingConfigKeys

class FundingConfigMigrationTest {

    private val fundingConfigMigration = FundingConfigMigration()

    @Test
    fun `migrate should be NoOp when not upgrading`() = runTest {
        val currentConfig = Config().apply {
            this[ConfigKey.StringKey(FundingConfigKeys.FUNDING_REMINDER_COUNT.name)] = "{\"id\":\"1\"}"
        }
        val migrationResult = fundingConfigMigration.migrate(
            currentVersion = 1,
            newVersion = 1,
            current = currentConfig,
        )
        assertThat(migrationResult).isEqualTo(ConfigMigrationResult.NoOp)
    }

    @Test
    fun `migrate should be NoOp on version 1`() = runTest {
        val current = Config()
        val result = fundingConfigMigration.migrate(currentVersion = 0, newVersion = 1, current = current)

        assertThat(result).isEqualTo(ConfigMigrationResult.NoOp)
    }

    @Test
    fun `migrate should reset data when upgrading from 1 to 2`() = runTest {
        val currentConfig = getVersionOneFundingShownConfig()
        val resetConfig = getDefaultFundingShownConfig()

        val migrationResult = fundingConfigMigration.migrate(
            currentVersion = 1,
            newVersion = 2,
            current = currentConfig,
        ) as ConfigMigrationResult.Migrated

        assertThat(migrationResult.updated != currentConfig)
        assertThat(migrationResult.updated == resetConfig)
        assertThat(migrationResult.keysToRemove.contains(
            FundingConfigKeys.DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT)
        )
        assertThat(
            migrationResult.updated[FundingConfigKeys.DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT]
        ).isNull()
    }

    companion object {
        const val REFERENCE_TIMESTAMP = 1000L
        const val ALL_REMINDERS_SEEN_COUNT = 2
        const val LAST_REMINDER_SEEN_TIMESTAMP = 3000L
        const val LAST_REMINDER_SEEN_ACTIVITY_TIMESTAMP = 30L
    }

    private fun getDefaultFundingShownConfig(): Config {
        return Config().apply {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = 0L
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = 0
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN] = 0L
        }
    }
    private fun getVersionOneFundingShownConfig(): Config {
        return Config().apply {
            this[FundingConfigKeys.REMINDER_REFERENCE_TIMESTAMP] = REFERENCE_TIMESTAMP
            this[FundingConfigKeys.FUNDING_REMINDER_COUNT] = ALL_REMINDERS_SEEN_COUNT
            this[FundingConfigKeys.LAST_FUNDING_REMINDER_SHOWN] = LAST_REMINDER_SEEN_TIMESTAMP
            this[FundingConfigKeys.DEPRECATED_LAST_FUNDING_REMINDER_SHOWN_ACTIVITY_AMOUNT] =
                LAST_REMINDER_SEEN_ACTIVITY_TIMESTAMP
        }
    }
}
