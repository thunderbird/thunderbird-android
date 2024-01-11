package app.k9mail.feature.account.server.certificate.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerNameFormatterTest {
    private val formatter = DefaultServerNameFormatter()

    @Test
    fun hostname() {
        assertThat(formatter.format("domain.example")).isEqualTo("domain.\u200Bexample")
    }

    @Test
    fun `IPv4 address`() {
        assertThat(formatter.format("127.0.0.1")).isEqualTo("127.\u200B0.\u200B0.\u200B1")
    }

    @Test
    fun `IPv6 address`() {
        assertThat(formatter.format("2001:db8::1"))
            .isEqualTo("2001:\u200B0db8:\u200B0000:\u200B0000:\u200B0000:\u200B0000:\u200B0000:\u200B0001")
    }
}
