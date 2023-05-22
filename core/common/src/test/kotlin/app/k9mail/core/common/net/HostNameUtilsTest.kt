package app.k9mail.core.common.net

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

/**
 * Test data copied from `mailnews/base/test/unit/test_hostnameUtils.js`
 */
class HostNameUtilsTest {
    @Test
    fun `valid host names`() {
        assertThat("localhost").isLegalHostName()
        assertThat("some-server").isLegalHostName()
        assertThat("server.company.invalid").isLegalHostName()
        assertThat("server.comp-any.invalid").isLegalHostName()
        assertThat("server.123.invalid").isLegalHostName()
        assertThat("1server.123.invalid").isLegalHostName()
        assertThat("1.2.3.4.5").isLegalHostName()
        assertThat("very.log.sub.domain.name.invalid").isLegalHostName()
        assertThat("1234567890").isLegalHostName()
        assertThat("1234567890.").isLegalHostName()
        assertThat("server.company.invalid.").isLegalHostName()
    }

    @Test
    fun `invalid host names`() {
        assertThat("").isNotLegalHostName()
        assertThat("server.badcompany!.invalid").isNotLegalHostName()
        assertThat("server._badcompany.invalid").isNotLegalHostName()
        assertThat("server.bad_company.invalid").isNotLegalHostName()
        assertThat("server.badcompany-.invalid").isNotLegalHostName()
        assertThat("server.bad company.invalid").isNotLegalHostName()
        assertThat("server.b…dcompany.invalid").isNotLegalHostName()
        assertThat(".server.badcompany.invalid").isNotLegalHostName()
        assertThat("make-this-a-long-host-name-component-that-is-over-63-characters-long.invalid").isNotLegalHostName()
        assertThat(
            "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid." +
                "append-strings-to-make-this-a-too-long-host-name.that-is-really-over-255-characters-long.invalid",
        ).isNotLegalHostName()
    }

    @Test
    fun `valid IPv4 addresses`() {
        assertThat("1.2.3.4").isLegalIPv4Address()
        assertThat("123.245.111.222").isLegalIPv4Address()
        assertThat("255.255.255.255").isLegalIPv4Address()
        assertThat("1.2.0.4").isLegalIPv4Address()
        assertThat("1.2.3.4").isLegalIPv4Address()
        assertThat("127.1.2.3").isLegalIPv4Address()
        assertThat("10.1.2.3").isLegalIPv4Address()
        assertThat("192.168.2.3").isLegalIPv4Address()
    }

    @Test
    fun `invalid IPv4 addresses`() {
        assertThat("1.2.3.4.5").isNotLegalIPv4Address()
        assertThat("1.2.3").isNotLegalIPv4Address()
        assertThat("1.2.3.").isNotLegalIPv4Address()
        assertThat(".1.2.3").isNotLegalIPv4Address()
        assertThat("1.2.3.256").isNotLegalIPv4Address()
        assertThat("1.2.3.12345").isNotLegalIPv4Address()
        assertThat("1.2..123").isNotLegalIPv4Address()
        assertThat("1").isNotLegalIPv4Address()
        assertThat("").isNotLegalIPv4Address()
        assertThat("0.1.2.3").isNotLegalIPv4Address()
        assertThat("0.0.2.3").isNotLegalIPv4Address()
        assertThat("0.0.0.0").isNotLegalIPv4Address()
        assertThat("1.2.3.d").isNotLegalIPv4Address()
        assertThat("a.b.c.d").isNotLegalIPv4Address()
        assertThat("a.b.c.d").isNotLegalIPv4Address()

        // Extended formats of IPv4, hex, octal, decimal up to DWORD
        // We intentionally don't support any of these.
        assertThat("0xff.0x12.0x45.0x78").isNotLegalIPv4Address()
        assertThat("01.0123.056.077").isNotLegalIPv4Address()
        assertThat("0xff.2.3.4").isNotLegalIPv4Address()
        assertThat("0xff.2.3.077").isNotLegalIPv4Address()
        assertThat("0x7f.2.3.077").isNotLegalIPv4Address()
        assertThat("0xZZ.1.2.3").isNotLegalIPv4Address()
        assertThat("0x00.0123.056.077").isNotLegalIPv4Address()
        assertThat("0x11.0123.056.078").isNotLegalIPv4Address()
        assertThat("0x11.0123.056.0789").isNotLegalIPv4Address()
        assertThat("1234566945").isNotLegalIPv4Address()
        assertThat("12345").isNotLegalIPv4Address()
        assertThat("123456789123456").isNotLegalIPv4Address()
        assertThat("127.1").isNotLegalIPv4Address()
        assertThat("0x7f.100").isNotLegalIPv4Address()
        assertThat("0x7f.100.1000").isNotLegalIPv4Address()
        assertThat("0xff.100.1024").isNotLegalIPv4Address()
        assertThat("0xC0.0xA8.0x2A48").isNotLegalIPv4Address()
        assertThat("0xC0.0xA82A48").isNotLegalIPv4Address()
        assertThat("0xC0A82A48").isNotLegalIPv4Address()
        assertThat("0324.062477106").isNotLegalIPv4Address()
        assertThat("0.0.1000").isNotLegalIPv4Address()
        assertThat("0324.06247710677").isNotLegalIPv4Address()
    }

    @Test
    fun `valid IPv6 addresses`() {
        assertThat("2001:0db8:85a3:0000:0000:8a2e:0370:7334").isNormalizedTo("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
        assertThat("2001:db8:85a3:0:0:8a2e:370:7334").isNormalizedTo("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
        assertThat("2001:db8:85a3::8a2e:370:7334").isNormalizedTo("2001:0db8:85a3:0000:0000:8a2e:0370:7334")
        assertThat("2001:0db8:85a3:0000:0000:8a2e:0370:").isNormalizedTo("2001:0db8:85a3:0000:0000:8a2e:0370:0000")
        assertThat("::ffff:c000:0280").isNormalizedTo("0000:0000:0000:0000:0000:ffff:c000:0280")
        assertThat("::ffff:192.0.2.128").isNormalizedTo("0000:0000:0000:0000:0000:ffff:c000:0280")
        assertThat("2001:db8::1").isNormalizedTo("2001:0db8:0000:0000:0000:0000:0000:0001")
        assertThat("2001:DB8::1").isNormalizedTo("2001:0db8:0000:0000:0000:0000:0000:0001")
        assertThat("1:2:3:4:5:6:7:8").isNormalizedTo("0001:0002:0003:0004:0005:0006:0007:0008")
        assertThat("::1").isNormalizedTo("0000:0000:0000:0000:0000:0000:0000:0001")
        assertThat("::0000:0000:1").isNormalizedTo("0000:0000:0000:0000:0000:0000:0000:0001")
    }

    @Test
    fun `invalid IPv6 addresses`() {
        assertThat("::").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:8a2e:0370:73346").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:8a2e:0370:7334:1").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:8a2e:0370:7334x").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:8a2e:03707334").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000x8a2e:0370:7334").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:::1").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:0000:some:junk").isNotLegalIPv6Address()
        assertThat("2001:0db8:85a3:0000:0000:0000::192.0.2.359").isNotLegalIPv6Address()
        assertThat("some::junk").isNotLegalIPv6Address()
        assertThat("some_junk").isNotLegalIPv6Address()
    }

    @Test
    fun cleanUpHostName() {
        assertThat(HostNameUtils.cleanUpHostName("imap.domain.example ")).isEqualTo("imap.domain.example")
    }
}

private fun Assert<String>.isLegalHostName() = given { actual ->
    assertThat(HostNameUtils.isLegalHostName(actual)).isNotNull()
    assertThat(HostNameUtils.isLegalHostNameOrIP(actual)).isNotNull()
}

private fun Assert<String>.isNotLegalHostName() = given { actual ->
    assertThat(HostNameUtils.isLegalHostName(actual)).isNull()
}

private fun Assert<String>.isLegalIPv4Address() = given { actual ->
    assertThat(HostNameUtils.isLegalIPv4Address(actual)).isNotNull()
    assertThat(HostNameUtils.isLegalIPAddress(actual)).isNotNull()
}

private fun Assert<String>.isNotLegalIPv4Address() = given { actual ->
    assertThat(HostNameUtils.isLegalIPv4Address(actual)).isNull()
}

private fun Assert<String>.isNormalizedTo(normalized: String) = given { actual ->
    assertThat(HostNameUtils.isLegalIPv6Address(actual)).isEqualTo(normalized)
    assertThat(HostNameUtils.isLegalHostNameOrIP(actual)).isEqualTo(normalized)
}

private fun Assert<String>.isNotLegalIPv6Address() = given { actual ->
    assertThat(HostNameUtils.isLegalIPv6Address(actual)).isNull()
}
