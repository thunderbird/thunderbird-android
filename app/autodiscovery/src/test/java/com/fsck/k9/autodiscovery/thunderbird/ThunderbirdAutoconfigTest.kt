package com.fsck.k9.autodiscovery.thunderbird

import com.fsck.k9.RobolectricTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThunderbirdAutoconfigTest : RobolectricTest() {
    private val parser = ThunderbirdAutoconfigParser()

    @Test
    fun settingsExtract() {
        val input = """<?xml version="1.0"?>
            <clientConfig version="1.1">
                <emailProvider id="metacode.biz">
                <domain>metacode.biz</domain>

                <incomingServer type="imap">
                    <hostname>imap.googlemail.com</hostname>
                    <port>993</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                </incomingServer>

                <outgoingServer type="smtp">
                    <hostname>smtp.googlemail.com</hostname>
                    <port>465</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                    <addThisServer>true</addThisServer>
                </outgoingServer>
            </emailProvider>
        </clientConfig>
        """.trimIndent().byteInputStream()

        val connectionSettings = parser.parseSettings(input, "test@metacode.biz")

        assertThat(connectionSettings).isNotNull()
        assertThat(connectionSettings!!.incoming).isNotNull()
        assertThat(connectionSettings.outgoing).isNotNull()
        with(connectionSettings.incoming) {
            assertThat(host).isEqualTo("imap.googlemail.com")
            assertThat(port).isEqualTo(993)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
        with(connectionSettings.outgoing) {
            assertThat(host).isEqualTo("smtp.googlemail.com")
            assertThat(port).isEqualTo(465)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
    }

    @Test
    fun pickFirstServer() {
        val input = """<?xml version="1.0"?>
            <clientConfig version="1.1">
                <emailProvider id="metacode.biz">
                <domain>metacode.biz</domain>

                <incomingServer type="imap">
                    <hostname>imap.googlemail.com</hostname>
                    <port>993</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                </incomingServer>

                <outgoingServer type="smtp">
                    <hostname>first</hostname>
                    <port>465</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                    <addThisServer>true</addThisServer>
                </outgoingServer>

                <outgoingServer type="smtp">
                    <hostname>second</hostname>
                    <port>465</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                    <addThisServer>true</addThisServer>
                </outgoingServer>
            </emailProvider>
        </clientConfig>
        """.trimIndent().byteInputStream()

        val connectionSettings = parser.parseSettings(input, "test@metacode.biz")

        assertThat(connectionSettings).isNotNull()
        assertThat(connectionSettings!!.outgoing).isNotNull()
        with(connectionSettings.outgoing) {
            assertThat(host).isEqualTo("first")
            assertThat(port).isEqualTo(465)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
    }

    @Test
    fun invalidResponse() {
        val input = """<?xml version="1.0"?>
            <clientConfig version="1.1">
                <emailProvider id="metacode.biz">
                    <domain>metacode.biz</domain>
        """.trimIndent().byteInputStream()

        val connectionSettings = parser.parseSettings(input, "test@metacode.biz")

        assertThat(connectionSettings).isNull()
    }

    @Test
    fun incompleteConfiguration() {
        val input = """<?xml version="1.0"?>
            <clientConfig version="1.1">
                <emailProvider id="metacode.biz">
                <domain>metacode.biz</domain>

                <incomingServer type="imap">
                    <hostname>imap.googlemail.com</hostname>
                    <port>993</port>
                    <socketType>SSL</socketType>
                    <username>%EMAILADDRESS%</username>
                    <authentication>OAuth2</authentication>
                    <authentication>password-cleartext</authentication>
                </incomingServer>
            </emailProvider>
        </clientConfig>
        """.trimIndent().byteInputStream()

        val connectionSettings = parser.parseSettings(input, "test@metacode.biz")

        assertThat(connectionSettings).isNull()
    }

    @Test
    fun generatedUrls() {
        val autoDiscoveryAddress = ThunderbirdAutoconfigFetcher.getAutodiscoveryAddress("test@metacode.biz")

        assertThat(autoDiscoveryAddress.toString()).isEqualTo("https://metacode.biz/" +
                ".well-known/autoconfig/mail/config-v1.1.xml?emailaddress=test%40metacode.biz")
    }
}
