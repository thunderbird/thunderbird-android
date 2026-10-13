package app.k9mail.feature.account.common.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.assertFailsWith
import org.junit.Test

class IncomingProtocolTypeTest {

    @Test
    fun `all should contain all protocol types`() {
        val protocolTypes = IncomingProtocolType.all()

        assertThat(protocolTypes).isEqualTo(
            IncomingProtocolType.entries,
        )
    }

    @Test
    fun `fromName should return right protocol type`() {
        val protocolType = IncomingProtocolType.fromName("imap")

        assertThat(protocolType).isEqualTo(IncomingProtocolType.IMAP)
    }

    @Test
    fun `fromName should return Graph protocol type`() {
        assertThat(IncomingProtocolType.fromName("graph")).isEqualTo(IncomingProtocolType.GRAPH)
    }

    @Test
    fun `fromName should return Gmail API protocol type`() {
        assertThat(IncomingProtocolType.fromName("gmailapi")).isEqualTo(IncomingProtocolType.GMAIL_API)
    }

    @Test
    fun `fromName should return Zoho Mail API protocol type`() {
        assertThat(IncomingProtocolType.fromName("zohomailapi")).isEqualTo(IncomingProtocolType.ZOHO_MAIL_API)
    }

    @Test
    fun `fromName should return Microsoft Graph protocol type for its provider ID`() {
        assertThat(IncomingProtocolType.fromName("msgraphapi")).isEqualTo(IncomingProtocolType.GRAPH)
    }

    @Test
    fun `fromName should throw IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { IncomingProtocolType.fromName("unknown") }
    }

    @Test
    fun `fromNameOrNull should return right protocol type`() {
        val protocolType = IncomingProtocolType.fromNameOrNull("pop3")

        assertThat(protocolType).isEqualTo(IncomingProtocolType.POP3)
    }

    @Test
    fun `fromNameOrNull should return null for unknown protocol name`() {
        val protocolType = IncomingProtocolType.fromNameOrNull("unknown")

        assertThat(protocolType).isNull()
    }

    @Test
    fun `defaultConnectionSecurity should provide right default connection security`() {
        val protocolTypeToConnectionSecurity = IncomingProtocolType.all()
            .associateWith { it.defaultConnectionSecurity }

        assertThat(protocolTypeToConnectionSecurity).isEqualTo(
            mapOf(
                IncomingProtocolType.IMAP to ConnectionSecurity.TLS,
                IncomingProtocolType.POP3 to ConnectionSecurity.TLS,
                IncomingProtocolType.GMAIL_API to ConnectionSecurity.TLS,
                IncomingProtocolType.GRAPH to ConnectionSecurity.TLS,
                IncomingProtocolType.ZOHO_MAIL_API to ConnectionSecurity.TLS,
            ),
        )
    }

    @Test
    fun `should provide right default port`() {
        val protocolTypeToPort = IncomingProtocolType.all()
            .associateWith { it.toDefaultPort(it.defaultConnectionSecurity) }

        assertThat(protocolTypeToPort).isEqualTo(
            mapOf(
                IncomingProtocolType.IMAP to 993L,
                IncomingProtocolType.POP3 to 995L,
                IncomingProtocolType.GMAIL_API to 443L,
                IncomingProtocolType.GRAPH to 443L,
                IncomingProtocolType.ZOHO_MAIL_API to 443L,
            ),
        )
    }
}
