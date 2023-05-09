package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.Test

class IspDbAutoconfigUrlProviderTest {
    private val urlProvider = IspDbAutoconfigUrlProvider()

    @Test
    fun `getAutoconfigUrls with ASCII email address`() {
        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain = "domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.thunderbird.net/v1.1/domain.example",
        )
    }
}
