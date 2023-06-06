package app.k9mail.feature.account.setup.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class IncomingProtocolTypeTest {

    @Test
    fun `should provide right default connection security`() {
        val incomingProtocolTypes = IncomingProtocolType.all()

        for (incomingProtocolType in incomingProtocolTypes) {
            val security = incomingProtocolType.defaultConnectionSecurity

            assertThat(security).isEqualTo(
                when (incomingProtocolType) {
                    IncomingProtocolType.IMAP -> ConnectionSecurity.TLS
                    IncomingProtocolType.POP3 -> ConnectionSecurity.TLS
                },
            )
        }
    }

    @Test
    fun `should provide right default port`() {
        val incomingProtocolTypes = IncomingProtocolType.all()

        for (incomingProtocolType in incomingProtocolTypes) {
            val port = incomingProtocolType.toDefaultPort(ConnectionSecurity.TLS)

            assertThat(port).isEqualTo(
                when (incomingProtocolType) {
                    IncomingProtocolType.IMAP -> 993L
                    IncomingProtocolType.POP3 -> 995L
                },
            )
        }
    }
}
