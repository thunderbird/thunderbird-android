package app.k9mail.feature.account.oauth.domain.usecase

import app.k9mail.core.common.mail.Protocols
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ServerNameSuggesterTest {
    private var serverNameSuggester: ServerNameSuggester? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        serverNameSuggester = ServerNameSuggester()
    }

    @Test
    @Throws(Exception::class)
    fun suggestServerName_forImapServer() {
        val serverType = Protocols.IMAP
        val domainPart = "example.org"
        val result = serverNameSuggester!!.suggestServerName(serverType, domainPart)
        Assert.assertEquals("imap.example.org", result)
    }

    @Test
    @Throws(Exception::class)
    fun suggestServerName_forPop3Server() {
        val serverType = Protocols.POP3
        val domainPart = "example.org"
        val result = serverNameSuggester!!.suggestServerName(serverType, domainPart)
        Assert.assertEquals("pop3.example.org", result)
    }

    @Test
    @Throws(Exception::class)
    fun suggestServerName_forSmtpServer() {
        val serverType = Protocols.SMTP
        val domainPart = "example.org"
        val result = serverNameSuggester!!.suggestServerName(serverType, domainPart)
        Assert.assertEquals("smtp.example.org", result)
    }
}
