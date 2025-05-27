package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.extracting
import net.thunderbird.core.common.mail.toEmailAddressOrThrow
import net.thunderbird.core.common.net.toDomain
import org.junit.Test

class PostMxLookupAutoconfigUrlProviderTest {
    @Test
    fun `getAutoconfigUrls with including email address`() {
        val urlProvider = createPostMxLookupAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = false, includeEmailAddress = true),
        )
        val domain = "domain.example".toDomain()
        val emailAddress = "test@domain.example".toEmailAddressOrThrow()

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, emailAddress)

        assertThat(autoconfigUrls).extracting { it.toString() }.containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "https://autoconfig.thunderbird.net/v1.1/domain.example",
        )
    }

    @Test
    fun `getAutoconfigUrls without including email address`() {
        val urlProvider = createPostMxLookupAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = false, includeEmailAddress = false),
        )
        val domain = "domain.example".toDomain()
        val emailAddress = "test@domain.example".toEmailAddressOrThrow()

        val autoconfigUrls = urlProvider.getAutoconfigUrls(domain, emailAddress)

        assertThat(autoconfigUrls).extracting { it.toString() }.containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml",
            "https://autoconfig.thunderbird.net/v1.1/domain.example",
        )
    }
}
