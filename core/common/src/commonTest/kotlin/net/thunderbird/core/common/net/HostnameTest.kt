package net.thunderbird.core.common.net

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.Test

class HostnameTest {
    @Test
    fun `valid domain`() {
        val hostname = Hostname("domain.example")

        assertThat(hostname.value).isEqualTo("domain.example")
    }

    @Test
    fun `valid IPv4`() {
        val hostname = Hostname("127.0.0.1")

        assertThat(hostname.value).isEqualTo("127.0.0.1")
    }

    @Test
    fun `valid IPv6`() {
        val hostname = Hostname("fc00::1")

        assertThat(hostname.value).isEqualTo("fc00::1")
    }

    @Test
    fun `invalid domain should throw`() {
        assertFailure {
            Hostname("invalid domain")
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a valid domain or IP: 'invalid domain'")
    }

    @Test
    fun `invalid IPv6 should throw`() {
        assertFailure {
            Hostname("fc00:1")
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a valid domain or IP: 'fc00:1'")
    }
}
