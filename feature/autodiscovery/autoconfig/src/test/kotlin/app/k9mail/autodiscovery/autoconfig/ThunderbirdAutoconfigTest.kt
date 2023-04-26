package app.k9mail.autodiscovery.autoconfig

import app.k9mail.autodiscovery.api.DiscoveredServerSettings
import app.k9mail.autodiscovery.api.DiscoveryResults
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import org.junit.Test

class ThunderbirdAutoconfigTest {
    private val parser = AutoconfigParser()

    @Test
    fun settingsExtract() {
        val input =
            """
            <?xml version="1.0"?>
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
        with(connectionSettings.incoming.first()) {
            assertThat(host).isEqualTo("imap.googlemail.com")
            assertThat(port).isEqualTo(993)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
        with(connectionSettings.outgoing.first()) {
            assertThat(host).isEqualTo("smtp.googlemail.com")
            assertThat(port).isEqualTo(465)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
    }

    @Test
    fun multipleServers() {
        val input =
            """
            <?xml version="1.0"?>
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

        val discoveryResults = parser.parseSettings(input, "test@metacode.biz")

        assertThat(discoveryResults).isNotNull()
        assertThat(discoveryResults!!.outgoing).isNotNull()
        with(discoveryResults.outgoing[0]) {
            assertThat(host).isEqualTo("first")
            assertThat(port).isEqualTo(465)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
        with(discoveryResults.outgoing[1]) {
            assertThat(host).isEqualTo("second")
            assertThat(port).isEqualTo(465)
            assertThat(username).isEqualTo("test@metacode.biz")
        }
    }

    @Test
    fun invalidResponse() {
        val input =
            """
            <?xml version="1.0"?>
                <clientConfig version="1.1">
                    <emailProvider id="metacode.biz">
                        <domain>metacode.biz</domain>
            """.trimIndent().byteInputStream()

        val connectionSettings = parser.parseSettings(input, "test@metacode.biz")

        assertThat(connectionSettings).isEqualTo(DiscoveryResults(listOf(), listOf()))
    }

    @Test
    fun incompleteConfiguration() {
        val input =
            """
            <?xml version="1.0"?>
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

        assertThat(connectionSettings).isEqualTo(
            DiscoveryResults(
                listOf(
                    DiscoveredServerSettings(
                        protocol = "imap",
                        host = "imap.googlemail.com",
                        port = 993,
                        security = ConnectionSecurity.SSL_TLS_REQUIRED,
                        authType = AuthType.PLAIN,
                        username = "test@metacode.biz",
                    ),
                ),
                listOf(),
            ),
        )
    }
}
