package app.k9mail.feature.account.common.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
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
    fun `fromName should throw IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> { IncomingProtocolType.fromName("unknown") }
    }

    @Test
    fun `defaultConnectionSecurity should provide right default connection security`() {
        val protocolTypeToConnectionSecurity = IncomingProtocolType.all()
            .associateWith { it.defaultConnectionSecurity }

        assertThat(protocolTypeToConnectionSecurity).isEqualTo(
            mapOf(
                IncomingProtocolType.IMAP to ConnectionSecurity.TLS,
                IncomingProtocolType.POP3 to ConnectionSecurity.TLS,
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
            ),
        )
    }
}
