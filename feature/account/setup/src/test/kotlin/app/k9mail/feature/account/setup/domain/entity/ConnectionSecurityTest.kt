package app.k9mail.feature.account.setup.domain.entity

import org.junit.Test

class ConnectionSecurityTest {

    @Test
    fun `should provide right default smtp port`() {
        val connectionSecurity = ConnectionSecurity.all()

        for (security in connectionSecurity) {
            val port = security.toSmtpDefaultPort()

            when (security) {
                ConnectionSecurity.None -> assert(port == 587L)
                ConnectionSecurity.StartTLS -> assert(port == 587L)
                ConnectionSecurity.TLS -> assert(port == 465L)
            }
        }
    }

    @Test
    fun `should provide right default imap port`() {
        val connectionSecurity = ConnectionSecurity.all()

        for (security in connectionSecurity) {
            val port = security.toImapDefaultPort()

            when (security) {
                ConnectionSecurity.None -> assert(port == 143L)
                ConnectionSecurity.StartTLS -> assert(port == 143L)
                ConnectionSecurity.TLS -> assert(port == 993L)
            }
        }
    }
}
