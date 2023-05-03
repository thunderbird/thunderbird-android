package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.Test

class ProviderAutoconfigUrlProviderTest {
    @Test
    fun `getAutoconfigUrls with http allowed and email address included`() {
        val urlProvider = ProviderAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = false, includeEmailAddress = true),
        )
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "http://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "http://domain.example/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress=test%40domain.example",
        )
    }

    @Test
    fun `getAutoconfigUrls with only https and email address included`() {
        val urlProvider = ProviderAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = true, includeEmailAddress = true),
        )
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml?emailaddress=test%40domain.example",
        )
    }

    @Test
    fun `getAutoconfigUrls with only https and email address not included`() {
        val urlProvider = ProviderAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = true, includeEmailAddress = false),
        )
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
        )
    }

    @Test
    fun `getAutoconfigUrls with http allowed and email address not included`() {
        val urlProvider = ProviderAutoconfigUrlProvider(
            AutoconfigUrlConfig(httpsOnly = false, includeEmailAddress = false),
        )
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
            "http://autoconfig.domain.example/mail/config-v1.1.xml",
            "http://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
        )
    }
}
