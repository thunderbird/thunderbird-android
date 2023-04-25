package com.fsck.k9.autodiscovery.thunderbird

import assertk.assertThat
import assertk.assertions.containsExactly
import org.junit.Test

class ThunderbirdAutoconfigUrlProviderTest {
    private val urlProvider = ThunderbirdAutoconfigUrlProvider()

    @Test
    fun `getAutoconfigUrls with ASCII email address`() {
        val autoconfigUrls = urlProvider.getAutoconfigUrls("test@domain.example")

        assertThat(autoconfigUrls.map { it.toString() }).containsExactly(
            "https://autoconfig.domain.example/mail/config-v1.1.xml?emailaddress=test%40domain.example",
            "https://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
            "http://domain.example/.well-known/autoconfig/mail/config-v1.1.xml",
            "https://autoconfig.thunderbird.net/v1.1/domain.example",
        )
    }
}
