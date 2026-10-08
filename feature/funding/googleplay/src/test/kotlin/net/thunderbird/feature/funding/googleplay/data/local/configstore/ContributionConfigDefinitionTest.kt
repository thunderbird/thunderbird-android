package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Test

class ContributionConfigDefinitionTest {

    private val mapper = ContributionConfigMapper(TestLogger())
    private val migration = ContributionConfigMigration()
    private val definition = ContributionConfigDefinition(
        mapper = mapper,
        migration = migration,
    )

    @Test
    fun `should have expected static properties`() {
        assertThat(definition.version).isEqualTo(1)
        assertThat(definition.id).isEqualTo(ConfigId(backend = "funding", feature = "contribution"))
        assertThat(definition.defaultValue).isEqualTo(ContributionConfig.DEFAULT)
        assertThat(definition.keys).containsExactly(ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION)
        assertThat(definition.migration).isInstanceOf(ContributionConfigMigration::class)
    }
}
