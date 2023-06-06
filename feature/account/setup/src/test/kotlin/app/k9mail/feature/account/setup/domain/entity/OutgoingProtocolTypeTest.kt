package app.k9mail.feature.account.setup.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OutgoingProtocolTypeTest {

    @Test
    fun `should provide right default connection security`() {
        val outgoingProtocolTypes = OutgoingProtocolType.all()

        for (protocolType in outgoingProtocolTypes) {
            val security = protocolType.defaultConnectionSecurity

            assertThat(security).isEqualTo(
                when (protocolType) {
                    OutgoingProtocolType.SMTP -> ConnectionSecurity.TLS
                },
            )
        }
    }

    @Test
    fun `should provide right default port`() {
        val outgoingProtocolTypes = OutgoingProtocolType.all()

        for (protocolType in outgoingProtocolTypes) {
            val port = protocolType.toDefaultPort(ConnectionSecurity.TLS)

            assertThat(port).isEqualTo(
                when (protocolType) {
                    OutgoingProtocolType.SMTP -> 465L
                },
            )
        }
    }
}
