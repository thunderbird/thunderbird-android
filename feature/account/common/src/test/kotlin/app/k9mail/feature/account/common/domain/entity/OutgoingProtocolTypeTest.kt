package app.k9mail.feature.account.common.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutgoingProtocolTypeTest {

    @Test
    fun `all should contain all protocol types`() {
        val protocolTypes = OutgoingProtocolType.all()

        assertThat(protocolTypes).isEqualTo(
            OutgoingProtocolType.entries,
        )
    }

    @Test
    fun `defaultConnectionSecurity should provide right default connection security`() {
        val protocolTypeToConnectionSecurity = OutgoingProtocolType.all().associateWith { it.defaultConnectionSecurity }

        assertThat(protocolTypeToConnectionSecurity).isEqualTo(
            mapOf(
                OutgoingProtocolType.SMTP to ConnectionSecurity.TLS,
            ),
        )
    }

    @Test
    fun `should provide right default port`() {
        val protocolTypeToPort = OutgoingProtocolType.all()
            .associateWith { it.toDefaultPort(it.defaultConnectionSecurity) }

        assertThat(protocolTypeToPort).isEqualTo(
            mapOf(
                OutgoingProtocolType.SMTP to 465L,
            ),
        )
    }
}
