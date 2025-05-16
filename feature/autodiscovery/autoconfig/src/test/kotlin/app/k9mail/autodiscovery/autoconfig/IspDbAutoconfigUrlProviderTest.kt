package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import net.thunderbird.core.common.net.toDomain
import org.junit.Test

class IspDbAutoconfigUrlProviderTest {
    private val urlProvider = IspDbAutoconfigUrlProvider()

    @Test
    fun `getAutoconfigUrls with ASCII email address`() {
        val domain = "domain.example".toDomain()

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain)

        assertThat(autoconfigUrls).extracting { it.toString() }.containsExactly(
            "https://autoconfig.thunderbird.net/v1.1/domain.example",
        )
    }
}
