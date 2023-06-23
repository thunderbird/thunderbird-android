package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.core.common.mail.Protocols
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class SuggestServerNameTest {

    private val testSubject = SuggestServerName()

    @Test
    fun `should suggest server name for IMAP server`() {
        val serverType = Protocols.IMAP
        val domain = "example.org"

        val result = testSubject.suggest(serverType, domain)

        assertThat(result).isEqualTo("imap.example.org")
    }

    @Test
    fun `should suggest server name for POP3 server`() {
        val serverType = Protocols.POP3
        val domain = "example.org"

        val result = testSubject.suggest(serverType, domain)

        assertThat(result).isEqualTo("pop3.example.org")
    }

    @Test
    fun `should suggest server name for SMTP server`() {
        val serverType = Protocols.SMTP
        val domain = "example.org"

        val result = testSubject.suggest(serverType, domain)

        assertThat(result).isEqualTo("smtp.example.org")
    }
}
