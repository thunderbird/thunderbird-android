package net.thunderbird.feature.funding.googleplay.data.local.configstore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import net.thunderbird.core.configstore.ConfigKey
import org.junit.Test

class ContributionConfigKeysTest {

    @Test
    fun `LAST_PURCHASED_CONTRIBUTION should have correct name and type`() {
        val key = ContributionConfigKeys.LAST_PURCHASED_CONTRIBUTION

        assertThat(key.name).isEqualTo("contribution_last_purchased_contribution")
        assertThat(key).isInstanceOf(ConfigKey.StringKey::class)
    }
}
