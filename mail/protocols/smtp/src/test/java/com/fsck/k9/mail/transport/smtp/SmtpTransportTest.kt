package com.fsck.k9.mail.transport.smtp

import assertk.Assert
import assertk.all
import assertk.assertFailure
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.MissingCapabilityException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.testing.XOAuth2ChallengeParserTestData
import com.fsck.k9.mail.testing.message.TestMessageBuilder
import com.fsck.k9.mail.testing.security.TestTrustedSocketFactory
import com.fsck.k9.mail.transport.mockServer.MockSmtpServer
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

private const val USERNAME = "user"
private const val PASSWORD = "password"
private val CLIENT_CERTIFICATE_ALIAS: String? = null

class SmtpTransportTest {
    private val socketFactory = TestTrustedSocketFactory
    private val oAuth2TokenProvider = createMockOAuth2TokenProvider()

    @Test
    fun `open() should issue EHLO command`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 OK")
        }
        val transport = startServerAndCreateSmtpTransportWithoutAuthentication(server)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() without AUTH LOGIN extension should connect when not using authentication`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 OK")
        }
        val transport = startServerAndCreateSmtpTransportWithoutAuthentication(server)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AUTH PLAIN extension`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH PLAIN LOGIN")
            expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.PLAIN)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AUTH LOGIN extension`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH LOGIN")
            expect("AUTH LOGIN")
            output("250 OK")
            expect("dXNlcg==")
            output("250 OK")
            expect("cGFzc3dvcmQ=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.PLAIN)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() without LOGIN and PLAIN AUTH extensions should throw`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH")
            expect("QUIT")
            output("221 BYE")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.PLAIN)

        assertFailure {
            transport.open()
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("AUTH PLAIN")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with CRAM-MD5 AUTH extension`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH CRAM-MD5")
            expect("AUTH CRAM-MD5")
            output("334 " + Base64.encode("<24609.1047914046@localhost>"))
            expect("dXNlciAyZDBlNTcwYzZlYWI0ZjY3ZDUyZmFkN2Q1NGExZDJhYQ==")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.CRAM_MD5)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() without CRAM-MD5 AUTH extension should throw`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH PLAIN LOGIN")
            expect("QUIT")
            output("221 BYE")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.CRAM_MD5)

        assertFailure {
            transport.open()
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("AUTH CRAM-MD5")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with OAUTHBEARER method`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH OAUTHBEARER")
            expect("AUTH OAUTHBEARER bixhPXVzZXIsAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with OAUTHBEARER method when XOAUTH2 method is also available`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2 OAUTHBEARER")
            expect("AUTH OAUTHBEARER bixhPXVzZXIsAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should throw on 401 response`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250-ENHANCEDSTATUSCODES")
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.STATUS_401_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("QUIT")
            output("221 BYE")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        assertFailure {
            transport.open()
        }.isInstanceOf<AuthenticationFailedException>()
            .hasServerMessage(
                "Username and Password not accepted. Learn more at " +
                    "http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68",
            )

        inOrder(oAuth2TokenProvider) {
            verify(oAuth2TokenProvider).getToken(anyLong())
            verify(oAuth2TokenProvider).invalidateToken()
        }
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should invalidate and retry on 400 response`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.STATUS_400_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        inOrder(oAuth2TokenProvider) {
            verify(oAuth2TokenProvider).getToken(anyLong())
            verify(oAuth2TokenProvider).invalidateToken()
            verify(oAuth2TokenProvider).getToken(anyLong())
        }
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should invalidate and retry on invalid JSON response`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.INVALID_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        inOrder(oAuth2TokenProvider) {
            verify(oAuth2TokenProvider).getToken(anyLong())
            verify(oAuth2TokenProvider).invalidateToken()
            verify(oAuth2TokenProvider).getToken(anyLong())
        }
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should invalidate and retry on missing status JSON response`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.MISSING_STATUS_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        inOrder(oAuth2TokenProvider) {
            verify(oAuth2TokenProvider).getToken(anyLong())
            verify(oAuth2TokenProvider).invalidateToken()
            verify(oAuth2TokenProvider).getToken(anyLong())
        }
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should throw on multiple failures`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250-ENHANCEDSTATUSCODES")
            output("250 AUTH XOAUTH2")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.STATUS_400_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=")
            output("334 " + XOAuth2ChallengeParserTestData.STATUS_400_RESPONSE)
            expect("")
            output("535-5.7.1 Username and Password not accepted. Learn more at")
            output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
            expect("QUIT")
            output("221 BYE")
        }

        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        assertFailure {
            transport.open()
        }.isInstanceOf<AuthenticationFailedException>()
            .hasServerMessage(
                "Username and Password not accepted. Learn more at " +
                    "http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68",
            )

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with XOAUTH2 extension should throw on failure to fetch token`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH XOAUTH2")
            expect("QUIT")
            output("221 BYE")
        }
        stubbing(oAuth2TokenProvider) {
            on { getToken(anyLong()) } doThrow AuthenticationFailedException("Failed to fetch token")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        assertFailure {
            transport.open()
        }.isInstanceOf<AuthenticationFailedException>()
            .hasMessage("Failed to fetch token")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() without OAUTHBEARER extension should throw`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH PLAIN LOGIN")
            expect("QUIT")
            output("221 BYE")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        assertFailure {
            transport.open()
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("AUTH OAUTHBEARER")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AUTH EXTERNAL extension`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH EXTERNAL")
            expect("AUTH EXTERNAL dXNlcg==")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.EXTERNAL)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() without AUTH EXTERNAL extension should throw`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH")
            expect("QUIT")
            output("221 BYE")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.EXTERNAL)

        assertFailure {
            transport.open()
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("AUTH EXTERNAL")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with EHLO failing should try HELO`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("502 5.5.1, Unrecognized command.")
            expect("HELO " + SMTP_HELLO_NAME)
            output("250 localhost")
        }
        val transport = startServerAndCreateSmtpTransportWithoutAuthentication(server)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with support for ENHANCEDSTATUSCODES should throw strip enhanced status codes from error message`() {
        val server = MockSmtpServer()
        server.output("220 localhost Simple Mail Transfer Service Ready")
        server.expect("EHLO " + SMTP_HELLO_NAME)
        server.output("250-localhost Hello " + SMTP_HELLO_NAME)
        server.output("250-ENHANCEDSTATUSCODES")
        server.output("250 AUTH XOAUTH2")
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
        server.output("334 " + XOAuth2ChallengeParserTestData.STATUS_401_RESPONSE)
        server.expect("")
        server.output("535-5.7.1 Username and Password not accepted. Learn more at")
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68")
        server.expect("QUIT")
        server.output("221 BYE")
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        assertFailure {
            transport.open()
        }.isInstanceOf<AuthenticationFailedException>()
            .hasServerMessage(
                "Username and Password not accepted. " +
                    "Learn more at http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68",
            )

        inOrder(oAuth2TokenProvider) {
            verify(oAuth2TokenProvider).getToken(anyLong())
            verify(oAuth2TokenProvider).invalidateToken()
        }
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with many extensions should parse all`() {
        val server = MockSmtpServer().apply {
            output("220 smtp.gmail.com ESMTP x25sm19117693wrx.27 - gsmtp")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-smtp.gmail.com at your service, [86.147.34.216]")
            output("250-SIZE 35882577")
            output("250-8BITMIME")
            output("250-AUTH LOGIN PLAIN XOAUTH2 PLAIN-CLIENTTOKEN XOAUTH")
            output("250-ENHANCEDSTATUSCODES")
            output("250-PIPELINING")
            output("250-CHUNKING")
            output("250 SMTPUTF8")
            expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(server, authenticationType = AuthType.XOAUTH2)

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with STARTTLS`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250-STARTTLS")
            output("250 HELP")
            expect("STARTTLS")
            output("220 Ready to start TLS")
            startTls()
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 AUTH PLAIN LOGIN")
            expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=")
            output("235 2.7.0 Authentication successful")
        }
        val transport = startServerAndCreateSmtpTransport(
            server,
            authenticationType = AuthType.PLAIN,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
        )

        transport.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with STARTTLS but without STARTTLS capability should throw`() {
        val server = MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)
            output("250 HELP")
            expect("QUIT")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(
            server,
            authenticationType = AuthType.PLAIN,
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED,
        )

        assertFailure {
            transport.open()
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("STARTTLS")

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() without address to send to should not open connection`() {
        val message = MimeMessage()
        val server = createServerAndSetupForPlainAuthentication()
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionNeverCreated()
    }

    @Test
    fun `sendMessage() with single recipient`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication().apply {
            expect("MAIL FROM:<user@localhost>")
            output("250 OK")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("250 OK: queued as 12345")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with 8-bit encoding`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication("8BITMIME").apply {
            expect("MAIL FROM:<user@localhost> BODY=8BITMIME")
            output("250 OK")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("250 OK: queued as 12345")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with 8-bit encoding extension not case-sensitive`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication("8bitmime").apply {
            expect("MAIL FROM:<user@localhost> BODY=8BITMIME")
            output("250 OK")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("250 OK: queued as 12345")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with message too large should throw`() {
        val message = createDefaultMessageBuilder()
            .setHasAttachments(true)
            .messageSize(1234L)
            .build()
        val server = createServerAndSetupForPlainAuthentication("SIZE 1000")
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<MessagingException>().all {
            hasMessage("Message too large for server")
            transform { it.isPermanentFailure }.isTrue()
        }

        // FIXME: Make sure connection was closed
        // server.verifyConnectionClosed();
    }

    @Test
    fun `sendMessage() with negative reply should throw`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication().apply {
            expect("MAIL FROM:<user@localhost>")
            output("250 OK")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("421 4.7.0 Temporary system problem")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<NegativeSmtpReplyException>().all {
            prop(NegativeSmtpReplyException::replyCode).isEqualTo(421)
            prop(NegativeSmtpReplyException::replyText).isEqualTo("4.7.0 Temporary system problem")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with pipelining`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication("PIPELINING").apply {
            expect("MAIL FROM:<user@localhost>")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("250 OK: queued as 12345")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() without pipelining`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication().apply {
            expect("MAIL FROM:<user@localhost>")
            output("250 OK")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            expect("DATA")
            output("354 End data with <CR><LF>.<CR><LF>")
            expect("[message data]")
            expect(".")
            output("250 OK: queued as 12345")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        transport.sendMessage(message)

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with pipelining and negative reply`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication("PIPELINING").apply {
            expect("MAIL FROM:<user@localhost>")
            expect("RCPT TO:<user2@localhost>")
            output("250 OK")
            output("550 remote mail to <user2@localhost> not allowed")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<NegativeSmtpReplyException>().all {
            prop(NegativeSmtpReplyException::replyCode).isEqualTo(550)
            prop(NegativeSmtpReplyException::replyText)
                .isEqualTo("remote mail to <user2@localhost> not allowed")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with pipelining and missing 354 reply for DATA should throw`() {
        val message = createDefaultMessage()
        val server = createServerAndSetupForPlainAuthentication("PIPELINING")
        server.expect("MAIL FROM:<user@localhost>")
        server.expect("RCPT TO:<user2@localhost>")
        server.output("250 OK")
        server.output("550 remote mail to <user2@localhost> not allowed")
        server.expect("QUIT")
        server.output("221 BYE")
        server.closeConnection()
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<NegativeSmtpReplyException>().all {
            prop(NegativeSmtpReplyException::replyCode).isEqualTo(550)
            prop(NegativeSmtpReplyException::replyText)
                .isEqualTo("remote mail to <user2@localhost> not allowed")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with pipelining and two 550 replies for recipients should include first error in exception`() {
        val message = createMessageWithTwoRecipients()
        val server = createServerAndSetupForPlainAuthentication("PIPELINING").apply {
            expect("MAIL FROM:<user@localhost>")
            expect("RCPT TO:<user2@localhost>")
            expect("RCPT TO:<user3@localhost>")
            output("250 OK")
            output("550 remote mail to <user2@localhost> not allowed")
            output("550 remote mail to <user3@localhost> not allowed")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<NegativeSmtpReplyException>().all {
            prop(NegativeSmtpReplyException::replyCode).isEqualTo(550)
            prop(NegativeSmtpReplyException::replyText)
                .isEqualTo("remote mail to <user2@localhost> not allowed")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `sendMessage() with pipelining and both 250 and 550 response for recipients should throw`() {
        val message = createMessageWithTwoRecipients()
        val server = createServerAndSetupForPlainAuthentication("PIPELINING").apply {
            expect("MAIL FROM:<user@localhost>")
            expect("RCPT TO:<user2@localhost>")
            expect("RCPT TO:<user3@localhost>")
            output("250 OK")
            output("250 OK")
            output("550 remote mail to <user3@localhost> not allowed")
            expect("QUIT")
            output("221 BYE")
            closeConnection()
        }
        val transport = startServerAndCreateSmtpTransport(server)

        assertFailure {
            transport.sendMessage(message)
        }.isInstanceOf<NegativeSmtpReplyException>().all {
            prop(NegativeSmtpReplyException::replyCode).isEqualTo(550)
            prop(NegativeSmtpReplyException::replyText)
                .isEqualTo("remote mail to <user3@localhost> not allowed")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    private fun startServerAndCreateSmtpTransportWithoutAuthentication(server: MockSmtpServer): SmtpTransport {
        return startServerAndCreateSmtpTransport(server, AuthType.NONE, ConnectionSecurity.NONE, password = null)
    }

    private fun startServerAndCreateSmtpTransport(
        server: MockSmtpServer,
        authenticationType: AuthType = AuthType.PLAIN,
        connectionSecurity: ConnectionSecurity = ConnectionSecurity.NONE,
        password: String? = PASSWORD,
    ): SmtpTransport {
        server.start()
        val host = server.host
        val port = server.port
        val serverSettings = ServerSettings(
            "smtp",
            host,
            port,
            connectionSecurity,
            authenticationType,
            USERNAME,
            password,
            CLIENT_CERTIFICATE_ALIAS,
        )

        return SmtpTransport(serverSettings, socketFactory, oAuth2TokenProvider)
    }

    private fun createDefaultMessageBuilder(): TestMessageBuilder {
        return TestMessageBuilder()
            .from("user@localhost")
            .to("user2@localhost")
    }

    private fun createDefaultMessage(): Message {
        return createDefaultMessageBuilder().build()
    }

    private fun createMessageWithTwoRecipients(): Message {
        return TestMessageBuilder()
            .from("user@localhost")
            .to("user2@localhost", "user3@localhost")
            .build()
    }

    private fun createServerAndSetupForPlainAuthentication(vararg extensions: String): MockSmtpServer {
        return MockSmtpServer().apply {
            output("220 localhost Simple Mail Transfer Service Ready")
            expect("EHLO " + SMTP_HELLO_NAME)
            output("250-localhost Hello " + SMTP_HELLO_NAME)

            for (extension in extensions) {
                output("250-$extension")
            }

            output("250 AUTH LOGIN PLAIN CRAM-MD5")
            expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=")
            output("235 2.7.0 Authentication successful")
        }
    }

    private fun createMockOAuth2TokenProvider(): OAuth2TokenProvider {
        return mock {
            on { getToken(anyLong()) } doReturn "oldToken" doReturn "newToken"
        }
    }
}

private fun Assert<AuthenticationFailedException>.hasServerMessage(expected: String) = given { actual ->
    assertThat(actual.messageFromServer).isEqualTo(expected)
}
