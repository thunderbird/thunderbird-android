package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey
import net.thunderbird.core.configstore.ConfigMigrationResult

class ContributionConfigMigrationTest {

    private val migration = ContributionConfigMigration()

    @Test
    fun `migrate should be NoOp when versions are equal`() = kotlinx.coroutines.test.runTest {
        val current = Config().apply {
            this[ConfigKey.StringKey("contribution_last_purchased_contribution")] = "{\"id\":\"1\"}"
        }

        val result = migration.migrate(currentVersion = 1, newVersion = 1, current = current)

        assertThat(result).isEqualTo(ConfigMigrationResult.NoOp)
    }

    @Test
    fun `migrate should be NoOp when upgrading from 0 to 1`() = kotlinx.coroutines.test.runTest {
        val current = Config()

        val result = migration.migrate(currentVersion = 0, newVersion = 1, current = current)

        assertThat(result).isEqualTo(ConfigMigrationResult.NoOp)
    }
}
