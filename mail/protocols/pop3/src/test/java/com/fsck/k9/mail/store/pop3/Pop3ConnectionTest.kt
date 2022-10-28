package com.fsck.k9.mail.store.pop3

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions

class Pop3ConnectionTest {
    private val settings = SimplePop3Settings()
    private val socketFactory = TestTrustedSocketFactory.newInstance()

    @Before
    fun before() {
        createCommonSettings()
    }

    @Test(expected = CertificateValidationException::class)
    fun `when TrustedSocketFactory throws SSLCertificateException, open() should throw CertificateValidationException`() {
        createTlsServer()
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow SSLException(CertificateException())
        }
        val connection = Pop3Connection(settings, mockSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws CertificateException, open() should throw MessagingException`() {
        createTlsServer()
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow SSLException("")
        }
        val connection = Pop3Connection(settings, mockSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws NoSuchAlgorithmException, open() should throw MessagingException`() {
        createTlsServer()
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow NoSuchAlgorithmException()
        }
        val connection = Pop3Connection(settings, mockSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws IOException, open() should throw MessagingException`() {
        createTlsServer()
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(null, settings.host, settings.port, null) } doThrow IOException()
        }
        val connection = Pop3Connection(settings, mockSocketFactory)

        connection.open()
    }

    @Test(expected = CertificateValidationException::class)
    fun `open() with STLS capability unavailable should throw CertificateValidationException`() {
        setupUnavailableStartTlsConnection()

        createAndOpenPop3Connection(settings, socketFactory)
    }

    @Test(expected = Pop3ErrorResponse::class)
    fun `open() with error response to STLS command should throw`() {
        setupFailedStartTlsConnection()

        createAndOpenPop3Connection(settings, socketFactory)
    }

    @Test
    fun `open() with STLS error response should not call createSocket() to upgrade to TLS`() {
        setupFailedStartTlsConnection()
        val mockSocketFactory = mock<TrustedSocketFactory>()

        try {
            createAndOpenPop3Connection(settings, mockSocketFactory)
        } catch (ignored: Exception) {
        }

        verifyNoInteractions(mockSocketFactory)
    }

    @Test(expected = MessagingException::class)
    fun `open() with StartTLS and TrustedSocketFactory throwing should throw`() {
        val server = setupStartTlsConnection()
        val mockSocketFactory = mock<TrustedSocketFactory> {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doThrow IOException()
        }

        createAndOpenPop3Connection(settings, mockSocketFactory)
    }

    @Test
    fun `open() with AUTH PLAIN`() {
        settings.authType = AuthType.PLAIN
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH PLAIN")
        server.output("+OK")
        server.expect(AUTH_PLAIN_ARGUMENT)
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication error should throw`() {
        settings.authType = AuthType.PLAIN
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH PLAIN")
        server.output("+OK")
        server.expect(AUTH_PLAIN_ARGUMENT)
        server.output("-ERR")

        try {
            startServerAndCreateOpenConnection(server)
            fail("Expected auth failure")
        } catch (ignored: AuthenticationFailedException) {
        }

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AuthType_PLAIN and no SASL PLAIN capability should use USER and PASS commands`() {
        settings.authType = AuthType.PLAIN
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("USER user")
        server.output("+OK")
        server.expect("PASS password")
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure during fallback to USER and PASS commands should throw`() {
        settings.authType = AuthType.PLAIN
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("USER user")
        server.output("+OK")
        server.expect("PASS password")
        server.output("-ERR")

        try {
            startServerAndCreateOpenConnection(server)
            fail("Expected auth failure")
        } catch (ignored: AuthenticationFailedException) {
        }

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with CRAM-MD5 authentication`() {
        settings.authType = AuthType.CRAM_MD5
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH CRAM-MD5")
        server.output("+ abcd")
        server.expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==")
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using CRAM-MD5 should throw`() {
        settings.authType = AuthType.CRAM_MD5
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH CRAM-MD5")
        server.output("+ abcd")
        server.expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==")
        server.output("-ERR")

        try {
            startServerAndCreateOpenConnection(server)
            fail("Expected auth failure")
        } catch (ignored: AuthenticationFailedException) {
        }

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with CRAM-MD5 configured but missing capability should use APOP`() {
        settings.authType = AuthType.CRAM_MD5
        val server = MockPop3Server()
        server.output("+OK abc<a>abcd")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN EXTERNAL")
        server.output(".")
        server.expect("APOP user c8e8c560e385faaa6367d4145572b8ea")
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using APOP should throw`() {
        settings.authType = AuthType.CRAM_MD5
        val server = MockPop3Server()
        server.output("+OK abc<a>abcd")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN EXTERNAL")
        server.output(".")
        server.expect("APOP user c8e8c560e385faaa6367d4145572b8ea")
        server.output("-ERR")

        try {
            startServerAndCreateOpenConnection(server)
            fail("Expected auth failure")
        } catch (ignored: AuthenticationFailedException) {
        }

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AUTH EXTERNAL`() {
        settings.authType = AuthType.EXTERNAL
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH EXTERNAL dXNlcg==")
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with AuthType_EXTERNAL configured but missing capability should throw`() {
        settings.authType = AuthType.EXTERNAL
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5")
        server.output(".")

        try {
            startServerAndCreateOpenConnection(server)
            fail("CVE expected")
        } catch (e: CertificateValidationException) {
            assertThat(e.reason).isEqualTo(CertificateValidationException.Reason.MissingCapability)
        }

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with authentication failure when using AUTH EXTERNAL should throw`() {
        settings.authType = AuthType.EXTERNAL
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN CRAM-MD5 EXTERNAL")
        server.output(".")
        server.expect("AUTH EXTERNAL dXNlcg==")
        server.output("-ERR Invalid certificate")

        try {
            startServerAndCreateOpenConnection(server)
            fail("CVE expected")
        } catch (e: CertificateValidationException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("POP3 client certificate authentication failed: -ERR Invalid certificate")
        }

        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with StartTLS and AUTH PLAIN`() {
        settings.authType = AuthType.PLAIN
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED
        val server = MockPop3Server()
        setupServerWithStartTlsAvailable(server)
        server.expect("STLS")
        server.output("+OK Begin TLS negotiation")
        server.startTls()
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN")
        server.output(".")
        server.expect("AUTH PLAIN")
        server.output("+OK")
        server.expect(AUTH_PLAIN_ARGUMENT)
        server.output("+OK")

        startServerAndCreateOpenConnection(server)

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    private fun createCommonSettings() {
        settings.username = USERNAME
        settings.password = PASSWORD
    }

    private fun startServerAndCreateOpenConnection(server: MockPop3Server) {
        server.start()
        settings.host = server.host
        settings.port = server.port
        createAndOpenPop3Connection(settings, socketFactory!!)
    }

    private fun createAndOpenPop3Connection(settings: Pop3Settings, socketFactory: TrustedSocketFactory) {
        val connection = Pop3Connection(settings, socketFactory)
        connection.open()
    }

    private fun setupStartTlsConnection(): MockPop3Server {
        val server = MockPop3Server()
        setupServerWithStartTlsAvailable(server)
        server.expect("STLS")
        server.output("+OK Begin TLS negotiation")
        server.start()

        settings.host = server.host
        settings.port = server.port
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED

        return server
    }

    private fun setupFailedStartTlsConnection(): MockPop3Server {
        val server = MockPop3Server()
        setupServerWithStartTlsAvailable(server)
        server.expect("STLS")
        server.output("-ERR Unavailable")
        server.start()

        settings.host = server.host
        settings.port = server.port
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED

        return server
    }

    private fun setupUnavailableStartTlsConnection(): MockPop3Server {
        val server = MockPop3Server()
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("SASL PLAIN")
        server.output(".")
        server.start()

        settings.host = server.host
        settings.port = server.port
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED

        return server
    }

    private fun setupServerWithStartTlsAvailable(server: MockPop3Server) {
        server.output("+OK POP3 server greeting")
        server.expect("CAPA")
        server.output("+OK Listing of supported mechanisms follows")
        server.output("STLS")
        server.output("SASL PLAIN")
        server.output(".")
    }

    private fun createTlsServer() {
        // MockPop3Server doesn't actually support implicit TLS. However, all tests using this method will encounter
        // an exception before sending the first command to the server.
        val server = MockPop3Server()
        server.start()

        settings.host = server.host
        settings.port = server.port
        settings.connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED
    }

    companion object {
        private const val USERNAME = "user"
        private const val PASSWORD = "password"

        private val AUTH_PLAIN_ARGUMENT = "\u0000$USERNAME\u0000$PASSWORD".encodeUtf8().base64()
    }
}
