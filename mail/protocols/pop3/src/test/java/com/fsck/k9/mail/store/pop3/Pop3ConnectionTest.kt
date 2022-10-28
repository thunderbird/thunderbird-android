package com.fsck.k9.mail.store.pop3

import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Socket
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stubbing
import org.mockito.kotlin.verify

class Pop3ConnectionTest {
    private var mockTrustedSocketFactory: TrustedSocketFactory? = null
    private var mockSocket: Socket? = null
    private val outputStreamForMockSocket = ByteArrayOutputStream()
    private val settings = SimplePop3Settings()
    private val socketFactory = TestTrustedSocketFactory.newInstance()

    @Before
    fun before() {
        createCommonSettings()
        createMocks()
    }

    @Test
    fun `constructor should not create socket`() {
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN

        Pop3Connection(settings, mockTrustedSocketFactory)

        verifyNoMoreInteractions(mockTrustedSocketFactory)
    }

    @Test(expected = CertificateValidationException::class)
    fun `when TrustedSocketFactory throws SSLCertificateException, open() should throw CertificateValidationException`() {
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(null, HOST, PORT, null) } doThrow SSLException(CertificateException())
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws CertificateException, open() should throw MessagingException`() {
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(null, HOST, PORT, null) } doThrow SSLException("")
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws NoSuchAlgorithmException, open() should throw MessagingException`() {
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(null, HOST, PORT, null) } doThrow NoSuchAlgorithmException("")
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `when TrustedSocketFactory throws IOException, open() should throw MessagingException`() {
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(null, HOST, PORT, null) } doThrow IOException("")
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()
    }

    @Test(expected = MessagingException::class)
    fun `open() with socket not connected should throw MessagingException`() {
        stubbing(mockSocket!!) {
            on { isConnected } doReturn false
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()
    }

    @Test
    fun `open() should send AUTH PLAIN command`() {
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }
        addSettingsForValidMockSocket()
        settings.authType = AuthType.PLAIN
        val connection = Pop3Connection(settings, mockTrustedSocketFactory)

        connection.open()

        assertThat(outputStreamForMockSocket.toByteArray().decodeToString()).isEqualTo(SUCCESSFUL_PLAIN_AUTH)
    }

    @Test(expected = CertificateValidationException::class)
    fun `open() with STLS capability unavailable should throw CertificateValidationException`() {
        setupUnavailableStartTlsConnection()
        settings.authType = AuthType.PLAIN
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)
    }

    @Test
    fun `open() with STLS capability unavailable should not call createSocket() to upgrade to TLS`() {
        setupUnavailableStartTlsConnection()
        settings.authType = AuthType.PLAIN
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED

        try {
            val connection = Pop3Connection(settings, mockTrustedSocketFactory)
            connection.open()
        } catch (ignored: Exception) {
        }

        verify(mockTrustedSocketFactory!!, never()).createSocket(any(), anyString(), anyInt(), anyString())
    }

    @Test(expected = Pop3ErrorResponse::class)
    fun `open() with error response to STLS command should throw`() {
        val server = setupFailedStartTlsConnection()
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doReturn mockSocket
        }
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)
    }

    @Test
    fun `open() with STLS error response should not call createSocket() to upgrade to TLS`() {
        val server = setupFailedStartTlsConnection()
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doReturn mockSocket
        }
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }

        try {
            createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)
        } catch (ignored: Exception) {
        }

        verify(mockTrustedSocketFactory!!, never()).createSocket(any(), anyString(), anyInt(), anyString())
    }

    @Test
    fun `open() with StartTLS should use TrustedSocketFactory to create TLS socket`() {
        val server = setupStartTLSConnection()
        settings.authType = AuthType.PLAIN
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doReturn mockSocket
        }
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)

        verify(mockTrustedSocketFactory!!).createSocket(any(), eq(server.host), eq(server.port), eq(null))
    }

    @Test(expected = MessagingException::class)
    fun `open() with StartTLS and TrustedSocketFactory throwing should throw`() {
        val server = setupStartTLSConnection()
        settings.authType = AuthType.PLAIN
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doThrow IOException()
        }
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)

        verify(mockTrustedSocketFactory!!).createSocket(any(), eq(server.host), eq(server.port), eq(null))
    }

    @Test
    fun `open() with StartTLS should authenticate over secure socket`() {
        val server = setupStartTLSConnection()
        settings.authType = AuthType.PLAIN
        stubbing(mockTrustedSocketFactory!!) {
            on { createSocket(any(), eq(server.host), eq(server.port), eq(null)) } doReturn mockSocket
        }
        stubbing(mockSocket!!) {
            on { getInputStream() } doReturn SUCCESSFUL_PLAIN_AUTH_RESPONSE.toByteArray().inputStream()
        }

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory!!)

        assertThat(outputStreamForMockSocket.toByteArray().decodeToString()).isEqualTo(SUCCESSFUL_PLAIN_AUTH)
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

    private fun createCommonSettings() {
        settings.username = USERNAME
        settings.password = PASSWORD
    }

    private fun createMocks() {
        mockSocket = mock {
            on { getOutputStream() } doReturn outputStreamForMockSocket
            on { isConnected } doReturn true
        }

        mockTrustedSocketFactory = mock {
            on { createSocket(null, HOST, PORT, null) } doReturn mockSocket
        }
    }

    private fun addSettingsForValidMockSocket() {
        settings.host = HOST
        settings.port = PORT
        settings.connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED
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

    private fun setupStartTLSConnection(): MockPop3Server {
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

    companion object {
        private const val HOST = "server"
        private const val PORT = 12345
        private const val USERNAME = "user"
        private const val PASSWORD = "password"

        private const val INITIAL_RESPONSE = "+OK POP3 server greeting\r\n"
        private const val CAPA_COMMAND = "CAPA\r\n"
        private const val CAPA_RESPONSE =
            "+OK Listing of supported mechanisms follows\r\n" +
                "SASL PLAIN CRAM-MD5 EXTERNAL\r\n" +
                ".\r\n"

        private val AUTH_PLAIN_ARGUMENT = "\u0000$USERNAME\u0000$PASSWORD".encodeUtf8().base64()
        private val AUTH_PLAIN_COMMAND = "AUTH PLAIN\r\n$AUTH_PLAIN_ARGUMENT\r\n"

        private const val AUTH_PLAIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n"

        private val SUCCESSFUL_PLAIN_AUTH =
            CAPA_COMMAND + AUTH_PLAIN_COMMAND

        private const val SUCCESSFUL_PLAIN_AUTH_RESPONSE =
            INITIAL_RESPONSE + CAPA_RESPONSE + AUTH_PLAIN_AUTHENTICATED_RESPONSE
    }
}
