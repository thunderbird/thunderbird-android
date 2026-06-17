package com.fsck.k9.mail

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test

class MailProxySettingsTest {
    @Test
    fun `fromExtra should return disabled proxy when proxy type is missing`() {
        val proxySettings = MailProxySettings.fromExtra(emptyMap())

        assertThat(proxySettings).isEqualTo(MailProxySettings.NONE)
    }

    @Test
    fun `fromExtra should parse SOCKS proxy settings`() {
        val proxySettings = MailProxySettings.fromExtra(
            mapOf(
                "proxy.type" to "socks",
                "proxy.host" to "127.0.0.1",
                "proxy.port" to "9050",
                "proxy.dns" to "true",
            ),
        )

        assertThat(proxySettings).isEqualTo(
            MailProxySettings(
                type = MailProxyType.SOCKS,
                host = "127.0.0.1",
                port = 9050,
                proxyDns = true,
            ),
        )
    }

    @Test
    fun `toExtra should serialize enabled proxy settings`() {
        val proxySettings = MailProxySettings(
            type = MailProxyType.HTTP,
            host = "proxy.example",
            port = 8080,
            proxyDns = false,
        )

        assertThat(proxySettings.toExtra()).isEqualTo(
            mapOf(
                "proxy.type" to "http",
                "proxy.host" to "proxy.example",
                "proxy.port" to "8080",
                "proxy.dns" to "false",
            ),
        )
    }

    @Test
    fun `creating enabled proxy with blank host should throw`() {
        assertFailure {
            MailProxySettings(type = MailProxyType.SOCKS, host = "", port = 9050)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("host must not be blank when proxy is enabled")
    }

    @Test
    fun `creating enabled proxy with invalid port should throw`() {
        assertFailure {
            MailProxySettings(type = MailProxyType.SOCKS, host = "127.0.0.1", port = 0)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("port must be in range 1..65535")
    }
}
