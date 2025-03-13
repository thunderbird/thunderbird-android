package com.fsck.k9.mail.store.pop3

import assertk.assertFailure
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthType.CRAM_MD5
import com.fsck.k9.mail.AuthType.EXTERNAL
import com.fsck.k9.mail.AuthType.PLAIN
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateChainException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ConnectionSecurity.NONE
import com.fsck.k9.mail.ConnectionSecurity.SSL_TLS_REQUIRED
import com.fsck.k9.mail.ConnectionSecurity.STARTTLS_REQUIRED
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.MissingCapabilityException
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.testing.security.TestTrustedSocketFactory
import java.io.IOException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLException
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class Pop3ConnectionTest {
    private val socketFactory = TestTrustedSocketFactory.newInstance()

    @Test
    fun `when TrustedSocketFactory throws wrapped CertificateChainException, open() should throw`() {
        val server = startTlsServer()
        val settings = server.createSettings(connectionSecurity = SSL_TLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow
                SSLException(CertificateChainException("irrelevant", arrayOf(), null))
        }

        assertFailure {
            createAndOpenPop3Connection(settings, mockSocketFactory)
        }.isInstanceOf<CertificateValidationException>()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws CertificateException, open() should throw MessagingException`() {
        val server = startTlsServer()
        val settings = server.createSettings(connectionSecurity = SSL_TLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow SSLException("")
        }

        createAndOpenPop3Connection(settings, mockSocketFactory)
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws NoSuchAlgorithmException, open() should throw MessagingException`() {
        val server = startTlsServer()
        val settings = server.createSettings(connectionSecurity = SSL_TLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow NoSuchAlgorithmException()
        }

        createAndOpenPop3Connection(settings, mockSocketFactory)
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws IOException, open() should throw MessagingException`() {
        val server = startTlsServer()
        val settings = server.createSettings(connectionSecurity = SSL_TLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow IOException()
        }

        createAndOpenPop3Connection(settings, mockSocketFactory)
    }

    @Test
    fun `open() with STLS capability unavailable should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN")
        }
        val settings = server.createSettings(connectionSecurity = STARTTLS_REQUIRED)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("STLS")
    }

    @Test(expected = Pop3ErrorResponse::class)
    fun `open() with error response to STLS command should throw`() {
        val server = startServer {
            setupServerWithStartTlsAvailable()
            expect("STLS")
            output("-ERR Unavailable")
        }
        val settings = server.createSettings(connectionSecurity = STARTTLS_REQUIRED)

        createAndOpenPop3Connection(settings)
    }

    @Test
    fun `open() with STLS error response should not call createSocket() to upgrade to TLS`() {
        val server = startServer {
            setupServerWithStartTlsAvailable()
            expect("STLS")
            output("-ERR Unavailable")
        }
        val settings = server.createSettings(connectionSecurity = STARTTLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory>()

        try {
            createAndOpenPop3Connection(settings, mockSocketFactory)
        } catch (ignored: Exception) {
        }

        verifyNoInteractions(mockSocketFactory)
    }

    @Test(expected = MessagingException::class)
    fun `open() with StartTLS and TrustedSocketFactory throwing should throw`() {
        val server = startServer {
            setupServerWithStartTlsAvailable()
            expect("STLS")
            output("+OK Begin TLS negotiation")
        }
        val settings = server.createSettings(connectionSecurity = STARTTLS_REQUIRED)
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(any(), eq(settings.host), eq(settings.port), eq(null)) } doThrow IOException()
        }

        createAndOpenPop3Connection(settings, mockSocketFactory)
    }

    @Test
    fun `open() with AUTH PLAIN`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5 EXTERNAL")
            expect("AUTH PLAIN")
            output("+OK")
            expect(AUTH_PLAIN_ARGUMENT)
            output("+OK")
        }
        val settings = server.createSettings(authType = PLAIN)

        createAndOpenPop3Connection(settings)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication error should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5 EXTERNAL")
            expect("AUTH PLAIN")
            output("+OK")
            expect(AUTH_PLAIN_ARGUMENT)
            output("-ERR")
        }
        val settings = server.createSettings(authType = PLAIN)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<AuthenticationFailedException>()

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AuthType_PLAIN and no SASL PLAIN capability should use USER and PASS commands`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("CRAM-MD5 EXTERNAL")
            expect("USER user")
            output("+OK")
            expect("PASS password")
            output("+OK")
        }
        val settings = server.createSettings(authType = PLAIN)

        createAndOpenPop3Connection(settings)

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure during fallback to USER and PASS commands should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("CRAM-MD5 EXTERNAL")
            expect("USER user")
            output("+OK")
            expect("PASS password")
            output("-ERR")
        }
        val settings = server.createSettings(authType = PLAIN)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<AuthenticationFailedException>()

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with CRAM-MD5 authentication`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5 EXTERNAL")
            expect("AUTH CRAM-MD5")
            output("+ abcd")
            expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==")
            output("+OK")
        }
        val settings = server.createSettings(authType = CRAM_MD5)

        createAndOpenPop3Connection(settings)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using CRAM-MD5 should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5 EXTERNAL")
            expect("AUTH CRAM-MD5")
            output("+ abcd")
            expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==")
            output("-ERR")
        }
        val settings = server.createSettings(authType = CRAM_MD5)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<AuthenticationFailedException>()

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with CRAM-MD5 configured but missing capability should use APOP`() {
        val server = startServer {
            output("+OK abc<a>abcd")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("SASL PLAIN EXTERNAL")
            output(".")
            expect("APOP user c8e8c560e385faaa6367d4145572b8ea")
            output("+OK")
        }
        val settings = server.createSettings(authType = CRAM_MD5)

        createAndOpenPop3Connection(settings)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using APOP should throw`() {
        val server = startServer {
            output("+OK abc<a>abcd")
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("SASL PLAIN EXTERNAL")
            output(".")
            expect("APOP user c8e8c560e385faaa6367d4145572b8ea")
            output("-ERR")
        }
        val settings = server.createSettings(authType = CRAM_MD5)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<AuthenticationFailedException>()

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AUTH EXTERNAL`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("CRAM-MD5 EXTERNAL")
            expect("AUTH EXTERNAL dXNlcg==")
            output("+OK")
        }
        val settings = server.createSettings(authType = EXTERNAL)

        createAndOpenPop3Connection(settings)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AuthType_EXTERNAL configured but missing capability should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5")
        }
        val settings = server.createSettings(authType = EXTERNAL)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<MissingCapabilityException>()
            .prop(MissingCapabilityException::capabilityName).isEqualTo("SASL EXTERNAL")

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using AUTH EXTERNAL should throw`() {
        val server = startServer {
            setupServerWithAuthenticationMethods("PLAIN CRAM-MD5 EXTERNAL")
            expect("AUTH EXTERNAL dXNlcg==")
            output("-ERR Invalid certificate")
        }
        val settings = server.createSettings(authType = EXTERNAL)

        assertFailure {
            createAndOpenPop3Connection(settings)
        }.isInstanceOf<AuthenticationFailedException>()

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with StartTLS and AUTH PLAIN`() {
        val server = startServer {
            setupServerWithStartTlsAvailable()
            expect("STLS")
            output("+OK Begin TLS negotiation")
            startTls()
            expect("CAPA")
            output("+OK Listing of supported mechanisms follows")
            output("SASL PLAIN")
            output(".")
            expect("AUTH PLAIN")
            output("+OK")
            expect(AUTH_PLAIN_ARGUMENT)
            output("+OK")
        }
        val settings = server.createSettings(authType = PLAIN, connectionSecurity = STARTTLS_REQUIRED)

        createAndOpenPop3Connection(settings)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    private fun createAndOpenPop3Connection(
        settings: Pop3Settings,
        trustedSocketFactory: TrustedSocketFactory = socketFactory,
    ) {
        val connection = Pop3Connection(settings, trustedSocketFactory)
        connection.open()
    }

    private fun MockPop3Server.setupServerWithAuthenticationMethods(authenticationMethods: String) {
        output("+OK POP3 server greeting")
        expect("CAPA")
        output("+OK Listing of supported mechanisms follows")
        output("SASL $authenticationMethods")
        output(".")
    }

    private fun MockPop3Server.setupServerWithStartTlsAvailable() {
        output("+OK POP3 server greeting")
        expect("CAPA")
        output("+OK Listing of supported mechanisms follows")
        output("STLS")
        output("SASL PLAIN")
        output(".")
    }

    private fun startTlsServer(): MockPop3Server {
        // MockPop3Server doesn't actually support implicit TLS. However, all tests using this method will encounter
        // an exception before sending the first command to the server.
        return startServer { }
    }

    private fun MockPop3Server.createSettings(
        authType: AuthType = PLAIN,
        connectionSecurity: ConnectionSecurity = NONE,
    ): Pop3Settings {
        return SimplePop3Settings().apply {
            username = USERNAME
            password = PASSWORD
            this.authType = authType
            host = this@createSettings.host
            port = this@createSettings.port
            this.connectionSecurity = connectionSecurity
        }
    }

    private fun startServer(block: MockPop3Server.() -> Unit): MockPop3Server {
        return MockPop3Server().apply(block).apply { start() }
    }

    companion object {
        private const val USERNAME = "user"
        private const val PASSWORD = "password"

        private val AUTH_PLAIN_ARGUMENT = "\u0000$USERNAME\u0000$PASSWORD".encodeUtf8().base64()
    }
}
