package com.fsck.k9.mail.store.imap

import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.CertificateValidationException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.K9MailLib
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.SystemOutLogger
import com.fsck.k9.mail.XOAuth2ChallengeParserTest
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory
import com.fsck.k9.mail.oauth.OAuth2TokenProvider
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import com.fsck.k9.mail.store.imap.mockserver.MockImapServer
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.UnknownHostException
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

private const val DEBUGGING = false

private const val USERNAME = "user"
private const val PASSWORD = "123456"

private const val SOCKET_CONNECT_TIMEOUT = 10000
private const val SOCKET_READ_TIMEOUT = 10000

private const val XOAUTH_TOKEN = "token"
private const val XOAUTH_TOKEN_2 = "token2"
private val XOAUTH_STRING = "user=$USERNAME\u0001auth=Bearer $XOAUTH_TOKEN\u0001\u0001".base64()
private val XOAUTH_STRING_RETRY = "user=$USERNAME\u0001auth=Bearer $XOAUTH_TOKEN_2\u0001\u0001".base64()
private val OAUTHBEARER_STRING = "n,a=$USERNAME,\u0001auth=Bearer $XOAUTH_TOKEN\u0001\u0001".base64()

class RealImapConnectionTest {
    private var socketFactory = TestTrustedSocketFactory.newInstance()
    private var oAuth2TokenProvider = TestTokenProvider()
    private var settings = SimpleImapSettings().apply {
        username = USERNAME
        password = PASSWORD
    }

    @Before
    fun setUp() {
        if (DEBUGGING) {
            Timber.logger = SystemOutLogger()
            K9MailLib.setDebug(true)
            K9MailLib.setDebugSensitive(true)
        }
    }

    @Test
    fun `open() with no capabilities in initial response should issue pre-auth capabilities command`() {
        val server = MockImapServer().apply {
            output("* OK example.org server")
            expect("1 CAPABILITY")
            output("* CAPABILITY IMAP4 IMAP4REV1 AUTH=PLAIN")
            output("1 OK CAPABILITY Completed")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with capabilities in initial response should not issue pre-auth capabilities command`() {
        val server = MockImapServer().apply {
            output("* OK [CAPABILITY IMAP4 IMAP4REV1 AUTH=PLAIN]")
            expect("1 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("1 OK Success")
            postAuthenticationDialogRequestingCapabilities(tag = 2)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() after close() was called should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog()
            expect("2 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("2 OK LOGIN completed")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)
        imapConnection.open()
        imapConnection.close()

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("open() called after close(). Check wrapped exception to see where close() was called.")
        }
    }

    @Test
    fun `open() AUTH PLAIN with login disabled should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "LOGINDISABLED")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("Server doesn't support unencrypted passwords using AUTH=PLAIN and LOGIN is disabled.")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN with authentication failure should fall back to LOGIN`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 NO Login Failure")
            expect("3 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("3 OK LOGIN completed")
            postAuthenticationDialogRequestingCapabilities(tag = 4)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN and LOGIN fallback with authentication failure should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 NO Login Failure")
            expect("3 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("3 NO Go away")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: AuthenticationFailedException) {
            // FIXME: improve exception message
            assertThat(e).hasMessageThat().contains("Go away")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN failure and disconnect should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 NO [UNAVAILABLE] Maximum number of connections from user+IP exceeded")
            closeConnection()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            assertThat(e).hasMessageThat().contains("Maximum number of connections from user+IP exceeded")
        }

        assertThat(imapConnection.isConnected).isFalse()
        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN with BYE response and connection close should throw AuthenticationFailedException`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("* BYE Go away")
            output("2 NO Login Failure")
            closeConnection()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: AuthenticationFailedException) {
            // FIXME: improve exception message
            assertThat(e).hasMessageThat().contains("Login Failure")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH PLAIN without AUTH PLAIN capability should use LOGIN command`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog()
            expect("2 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("2 OK LOGIN completed")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH CRAM-MD5`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=CRAM-MD5")
            expect("2 AUTHENTICATE CRAM-MD5")
            output("+ ${"<0000.000000000@example.org>".base64()}")
            expect("dXNlciA2ZjdiOTcyYjk5YTI4NDk4OTRhN2YyMmE3MGRhZDg0OQ==")
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.CRAM_MD5)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH CRAM-MD5 with authentication failure should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=CRAM-MD5")
            expect("2 AUTHENTICATE CRAM-MD5")
            output("+ ${"<0000.000000000@example.org>".base64()}")
            expect("dXNlciA2ZjdiOTcyYjk5YTI4NDk4OTRhN2YyMmE3MGRhZDg0OQ==")
            output("2 NO Who are you?")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.CRAM_MD5)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: AuthenticationFailedException) {
            // FIXME: improve exception message
            assertThat(e).hasMessageThat().contains("Who are you?")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH CRAM-MD5 without AUTH CRAM-MD5 capability should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.CRAM_MD5)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertThat(e).hasMessageThat().isEqualTo("Server doesn't support encrypted passwords using CRAM-MD5.")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH OAUTHBEARER`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=OAUTHBEARER")
            expect("2 AUTHENTICATE OAUTHBEARER $OAUTHBEARER_STRING")
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH OAUTHBEARER when AUTH=XOAUTH2 and AUTH=OAUTHBEARER capabilities are present`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH2 AUTH=OAUTHBEARER")
            expect("2 AUTHENTICATE OAUTHBEARER $OAUTHBEARER_STRING")
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 with SASL-IR`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 with untagged CAPABILITY response after authentication`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("* CAPABILITY IMAP4rev1 X-GM-EXT-1")
            output("2 OK Success")
            simplePostAuthenticationDialog(tag = 3)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        assertThat(imapConnection.hasCapability("X-GM-EXT-1")).isTrue()
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 throws exception on 401 response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("+ ${XOAuth2ChallengeParserTest.STATUS_401_RESPONSE}")
            expect("")
            output("2 NO SASL authentication failed")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        try {
            imapConnection.open()
            fail()
        } catch (e: AuthenticationFailedException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("Command: AUTHENTICATE XOAUTH2; response: #2# [NO, SASL authentication failed]")
        }
    }

    @Test
    fun `open() AUTH XOAUTH2 invalidates and retries new token on 400 response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("+ ${XOAuth2ChallengeParserTest.STATUS_400_RESPONSE}")
            expect("")
            output("2 NO SASL authentication failed")
            expect("3 AUTHENTICATE XOAUTH2 $XOAUTH_STRING_RETRY")
            output("3 OK Success")
            postAuthenticationDialogRequestingCapabilities(tag = 4)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 invalidates and retries new token on invalid JSON response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("+ ${XOAuth2ChallengeParserTest.INVALID_RESPONSE}")
            expect("")
            output("2 NO SASL authentication failed")
            expect("3 AUTHENTICATE XOAUTH2 $XOAUTH_STRING_RETRY")
            output("3 OK Success")
            requestCapabilities(tag = 4)
            simplePostAuthenticationDialog(tag = 5)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 invalidates and retries new token on missing status JSON response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("+ ${XOAuth2ChallengeParserTest.MISSING_STATUS_RESPONSE}")
            expect("")
            output("2 NO SASL authentication failed")
            expect("3 AUTHENTICATE XOAUTH2 $XOAUTH_STRING_RETRY")
            output("3 OK Success")
            requestCapabilities(tag = 4)
            simplePostAuthenticationDialog(tag = 5)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH XOAUTH2 with old token throws exception if retry fails`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("+ r3j3krj3irj3oir3ojo")
            expect("")
            output("2 NO SASL authentication failed")
            expect("3 AUTHENTICATE XOAUTH2 $XOAUTH_STRING_RETRY")
            output("+ 433ba3a3a")
            expect("")
            output("3 NO SASL authentication failed")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        try {
            imapConnection.open()
            fail()
        } catch (e: AuthenticationFailedException) {
            assertThat(e).hasMessageThat()
                .isEqualTo("Command: AUTHENTICATE XOAUTH2; response: #3# [NO, SASL authentication failed]")
        }
    }

    @Test
    fun `open() AUTH XOAUTH2 parses capabilities`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "SASL-IR AUTH=XOAUTH AUTH=XOAUTH2")
            expect("2 AUTHENTICATE XOAUTH2 $XOAUTH_STRING")
            output("2 OK [CAPABILITY IMAP4REV1 IDLE XM-GM-EXT-1]")
            simplePostAuthenticationDialog(tag = 3)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.XOAUTH2)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
        assertThat(imapConnection.hasCapability("XM-GM-EXT-1")).isTrue()
    }

    @Test
    fun `open() AUTH EXTERNAL`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=EXTERNAL")
            expect("2 AUTHENTICATE EXTERNAL ${USERNAME.base64()}")
            output("2 OK Success")
            postAuthenticationDialogRequestingCapabilities()
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.EXTERNAL)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH EXTERNAL with authentication failure should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=EXTERNAL")
            expect("2 AUTHENTICATE EXTERNAL ${USERNAME.base64()}")
            output("2 NO Bad certificate")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.EXTERNAL)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: CertificateValidationException) {
            // FIXME: improve exception message
            assertThat(e).hasMessageThat().contains("Bad certificate")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() AUTH EXTERNAL without AUTH EXTERNAL capability should throw`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.EXTERNAL)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: CertificateValidationException) {
            assertThat(e.reason).isEqualTo(CertificateValidationException.Reason.MissingCapability)
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with no post-auth CAPABILITY response should issue CAPABILITY command`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 OK Success")
            expect("3 CAPABILITY")
            output("* CAPABILITY IDLE")
            output("3 OK CAPABILITY Completed")
            simplePostAuthenticationDialog(tag = 4)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
        assertThat(imapConnection.isIdleCapable).isTrue()
    }

    @Test
    fun `open() with untagged post-auth CAPABILITY response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output(
                "* CAPABILITY IMAP4rev1 UNSELECT IDLE QUOTA ID XLIST CHILDREN X-GM-EXT-1 UIDPLUS " +
                    "ENABLE MOVE CONDSTORE ESEARCH UTF8=ACCEPT LIST-EXTENDED LIST-STATUS LITERAL- SPECIAL-USE " +
                    "APPENDLIMIT=35651584"
            )
            output("2 OK")
            simplePostAuthenticationDialog(tag = 3)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
        assertThat(imapConnection.isIdleCapable).isTrue()
    }

    @Test
    fun `open() with post-auth CAPABILITY response`() {
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "AUTH=PLAIN")
            expect("2 AUTHENTICATE PLAIN")
            output("+")
            expect("\u0000$USERNAME\u0000$PASSWORD".base64())
            output("2 OK [CAPABILITY IDLE]")
            simplePostAuthenticationDialog(tag = 3)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
        assertThat(imapConnection.isIdleCapable).isTrue()
    }

    @Test
    fun `open() with NAMESPACE capability should issue NAMESPACE command`() {
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog(postAuthCapabilities = "NAMESPACE")
            expect("3 NAMESPACE")
            output("* NAMESPACE ((\"\" \"/\")) NIL NIL")
            output("3 OK command completed")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with connection error should throw`() {
        settings.host = "127.1.2.3"
        settings.port = 143
        val imapConnection = createImapConnection(settings, socketFactory, oAuth2TokenProvider)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: MessagingException) {
            assertThat(e).hasMessageThat().isEqualTo("Cannot connect to host")
            assertThat(e).hasCauseThat().isInstanceOf(IOException::class.java)
        }
    }

    @Test
    fun `open() with invalid hostname should throw`() {
        settings.host = "host name"
        settings.port = 143
        val imapConnection = createImapConnection(settings, socketFactory, oAuth2TokenProvider)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (ignored: UnknownHostException) {
        }

        assertThat(imapConnection.isConnected).isFalse()
    }

    @Test
    fun `open() with STARTTLS capability should issue STARTTLS command`() {
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "STARTTLS LOGINDISABLED")
            expect("2 STARTTLS")
            output("2 OK [CAPABILITY IMAP4REV1 NAMESPACE]")
            startTls()
            expect("3 CAPABILITY")
            output("* CAPABILITY IMAP4 IMAP4REV1")
            output("3 OK")
            expect("4 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("4 OK [CAPABILITY NAMESPACE] LOGIN completed")
            expect("5 NAMESPACE")
            output("* NAMESPACE ((\"\" \"/\")) NIL NIL")
            output("5 OK command completed")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with STARTTLS but without STARTTLS capability should throw`() {
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED
        val server = MockImapServer().apply {
            preAuthenticationDialog()
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: CertificateValidationException) {
            // FIXME: CertificateValidationException seems wrong
            assertThat(e).hasMessageThat().isEqualTo("STARTTLS connection security not available")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with untagged CAPABILITY after STARTTLS should not throw`() {
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "STARTTLS LOGINDISABLED")
            expect("2 STARTTLS")
            output("2 OK Begin TLS negotiation now")
            startTls()
            output("* CAPABILITY IMAP4REV1 IMAP4")
            expect("3 CAPABILITY")
            output("* CAPABILITY IMAP4 IMAP4REV1")
            output("3 OK")
            expect("4 LOGIN \"$USERNAME\" \"$PASSWORD\"")
            output("4 OK [CAPABILITY IMAP4REV1] LOGIN completed")
            simplePostAuthenticationDialog(tag = 5)
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with negative response to STARTTLS command should throw`() {
        settings.connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED
        val server = MockImapServer().apply {
            preAuthenticationDialog(capabilities = "STARTTLS")
            expect("2 STARTTLS")
            output("2 NO")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        try {
            imapConnection.open()
            fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            assertThat(e).hasMessageThat().isEqualTo("Command: STARTTLS; response: #2# [NO]")
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with COMPRESS=DEFLATE capability should enable compression`() {
        settings.setUseCompression(true)
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog(postAuthCapabilities = "COMPRESS=DEFLATE")
            expect("3 COMPRESS DEFLATE")
            output("3 OK")
            enableCompression()
            simplePostAuthenticationDialog(tag = 4)
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with negative response to COMPRESS command should continue`() {
        settings.setUseCompression(true)
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog(postAuthCapabilities = "COMPRESS=DEFLATE")
            expect("3 COMPRESS DEFLATE")
            output("3 NO")
            simplePostAuthenticationDialog(tag = 4)
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with IOException during COMPRESS command should throw`() {
        settings.setUseCompression(true)
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog(postAuthCapabilities = "COMPRESS=DEFLATE")
            expect("3 COMPRESS DEFLATE")
            closeConnection()
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        try {
            imapConnection.open()
            fail("Exception expected")
        } catch (ignored: IOException) {
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with IOException during LIST command should throw`() {
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog()
            expect("3 LIST \"\" \"\"")
            output("* Now what?")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        try {
            imapConnection.open()
            fail("Exception expected")
        } catch (ignored: IOException) {
        }

        server.verifyConnectionClosed()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `open() with negative response to LIST command`() {
        val server = MockImapServer().apply {
            simplePreAuthAndLoginDialog()
            expect("3 LIST \"\" \"\"")
            output("3 NO")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        imapConnection.open()

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `isConnected without previous open() should return false`() {
        val imapConnection = createImapConnection(settings, socketFactory, oAuth2TokenProvider)

        val result = imapConnection.isConnected

        assertThat(result).isFalse()
    }

    @Test
    fun `isConnected after open() should return true`() {
        val server = MockImapServer()
        val imapConnection = simpleOpen(server)

        val result = imapConnection.isConnected

        assertThat(result).isTrue()
        server.verifyConnectionStillOpen()
        server.shutdown()
    }

    @Test
    fun isConnected_afterOpenAndClose_shouldReturnFalse() {
        val server = MockImapServer()
        val imapConnection = simpleOpen(server)
        imapConnection.close()

        val result = imapConnection.isConnected

        assertThat(result).isFalse()
        server.verifyConnectionClosed()
        server.shutdown()
    }

    @Test
    fun `close() without open() should not throw`() {
        val imapConnection = createImapConnection(settings, socketFactory, oAuth2TokenProvider)

        imapConnection.close()
    }

    @Test
    fun `close() after open() should close connection`() {
        val server = MockImapServer()
        val imapConnection = simpleOpen(server)

        imapConnection.close()

        server.verifyConnectionClosed()
        server.shutdown()
    }

    @Test
    fun `isIdleCapable without IDLE capability should return false`() {
        val server = MockImapServer()
        val imapConnection = simpleOpen(server)

        val result = imapConnection.isIdleCapable

        assertThat(result).isFalse()
        server.shutdown()
    }

    @Test
    fun `isIdleCapable with IDLE capability should return true`() {
        val server = MockImapServer()
        val imapConnection = simpleOpenWithCapabilities(server, postAuthCapabilities = "IDLE")

        val result = imapConnection.isIdleCapable

        assertThat(result).isTrue()
        server.shutdown()
    }

    @Test
    fun `sendContinuation() should send line without tag`() {
        val server = MockImapServer().apply {
            simpleOpenDialog(postAuthCapabilities = "IDLE")
            expect("4 IDLE")
            output("+ idling")
            expect("DONE")
        }
        val imapConnection = startServerAndCreateImapConnection(server, authType = AuthType.PLAIN)

        imapConnection.open()
        imapConnection.sendCommand("IDLE", false)
        imapConnection.readResponse()
        imapConnection.sendContinuation("DONE")

        server.waitForInteractionToComplete()
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `executeSimpleCommand() with OK response should return result`() {
        val server = MockImapServer().apply {
            simpleOpenDialog()
            expect("4 CREATE Folder")
            output("4 OK Folder created")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        val result = imapConnection.executeSimpleCommand("CREATE Folder")

        assertThat(result).hasSize(1)
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `executeSimpleCommand() with NO response should throw NegativeImapResponseException`() {
        val server = MockImapServer().apply {
            simpleOpenDialog()
            expect("4 CREATE Folder")
            output("4 NO Folder exists")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        try {
            imapConnection.executeSimpleCommand("CREATE Folder")
            fail("Expected exception")
        } catch (e: NegativeImapResponseException) {
            assertThat(e.lastResponse).containsExactly("NO", "Folder exists")
        }

        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    @Test
    fun `hasCapability() with not yet opened connection should connect and fetch capabilities`() {
        val server = MockImapServer().apply {
            simpleOpenDialog(postAuthCapabilities = "X-SOMETHING")
        }
        val imapConnection = startServerAndCreateImapConnection(server)

        val capabilityPresent = imapConnection.hasCapability("X-SOMETHING")

        assertThat(capabilityPresent).isTrue()
        server.verifyConnectionStillOpen()
        server.verifyInteractionCompleted()
    }

    private fun createImapConnection(
        settings: ImapSettings,
        socketFactory: TrustedSocketFactory,
        oAuth2TokenProvider: OAuth2TokenProvider
    ): ImapConnection {
        val connectionGeneration = 1
        return RealImapConnection(
            settings,
            socketFactory,
            oAuth2TokenProvider,
            SOCKET_CONNECT_TIMEOUT,
            SOCKET_READ_TIMEOUT,
            connectionGeneration
        )
    }

    private fun startServerAndCreateImapConnection(
        server: MockImapServer,
        authType: AuthType = AuthType.PLAIN
    ): ImapConnection {
        server.start()
        settings.host = server.host
        settings.port = server.port
        settings.authType = authType

        return createImapConnection(settings, socketFactory, oAuth2TokenProvider)
    }

    private fun simpleOpen(server: MockImapServer): ImapConnection {
        return simpleOpenWithCapabilities(server, postAuthCapabilities = "")
    }

    private fun simpleOpenWithCapabilities(server: MockImapServer, postAuthCapabilities: String): ImapConnection {
        server.simpleOpenDialog(postAuthCapabilities)

        val imapConnection = startServerAndCreateImapConnection(server)
        imapConnection.open()

        return imapConnection
    }

    private fun MockImapServer.preAuthenticationDialog(capabilities: String = "") {
        output("* OK IMAP4rev1 Service Ready")
        expect("1 CAPABILITY")
        output("* CAPABILITY IMAP4 IMAP4REV1 $capabilities")
        output("1 OK CAPABILITY")
    }

    private fun MockImapServer.postAuthenticationDialogRequestingCapabilities(tag: Int = 3) {
        requestCapabilities(tag)
        simplePostAuthenticationDialog(tag + 1)
    }

    private fun MockImapServer.requestCapabilities(tag: Int) {
        expect("$tag CAPABILITY")
        output("* CAPABILITY IMAP4 IMAP4REV1 ")
        output("$tag OK CAPABILITY")
    }

    private fun MockImapServer.simplePostAuthenticationDialog(tag: Int) {
        expect("$tag LIST \"\" \"\"")
        output("* LIST () \"/\" foo/bar")
        output("$tag OK")
    }

    private fun MockImapServer.simpleOpenDialog(postAuthCapabilities: String = "") {
        simplePreAuthAndLoginDialog(postAuthCapabilities)
        simplePostAuthenticationDialog(3)
    }

    private fun MockImapServer.simplePreAuthAndLoginDialog(postAuthCapabilities: String = "") {
        settings.authType = AuthType.PLAIN
        preAuthenticationDialog()
        expect("2 LOGIN \"$USERNAME\" \"$PASSWORD\"")
        output("2 OK [CAPABILITY $postAuthCapabilities] LOGIN completed")
    }
}

class TestTokenProvider : OAuth2TokenProvider {
    private var invalidationCount = 0

    override fun getToken(timeoutMillis: Long): String {
        assertThat(timeoutMillis).isEqualTo(OAuth2TokenProvider.OAUTH2_TIMEOUT.toLong())

        return when (invalidationCount) {
            0 -> XOAUTH_TOKEN
            1 -> XOAUTH_TOKEN_2
            else -> {
                throw AuthenticationFailedException(
                    "Ran out of auth tokens. invalidateToken() called too often?"
                )
            }
        }
    }

    override fun invalidateToken() {
        invalidationCount++
    }
}

private fun String.base64() = this.encodeUtf8().base64()
