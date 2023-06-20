package app.k9mail.feature.account.setup.domain.entity

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class AutoDiscoveryConnectionSecurityKtTest {

    @Test
    fun `should map all AutoDiscoveryConnectionSecurities`() {
        val securities = AutoDiscoveryConnectionSecurity.values()

        for (security in securities) {
            val connectionSecurity = security.toConnectionSecurity()

            assertThat(connectionSecurity).isEqualTo(
                when (security) {
                    AutoDiscoveryConnectionSecurity.StartTLS -> ConnectionSecurity.StartTLS
                    AutoDiscoveryConnectionSecurity.TLS -> ConnectionSecurity.TLS
                },
            )
        }
    }
}
