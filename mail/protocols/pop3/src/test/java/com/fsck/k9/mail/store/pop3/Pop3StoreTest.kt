package com.fsck.k9.mail.store.pop3

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.ssl.TrustedSocketFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.Socket
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stubbing

class Pop3StoreTest {
    private val trustedSocketFactory = mock<TrustedSocketFactory>()
    private val store: Pop3Store = Pop3Store(createServerSettings(), trustedSocketFactory)

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `getFolder() should return same instance every time`() {
        val folderOne = store.getFolder("TestFolder")
        val folderTwo = store.getFolder("TestFolder")

        assertThat(folderTwo).isSameInstanceAs(folderOne)
    }

    @Test
    fun `getFolder() should return folder with correct server ID`() {
        val folder = store.getFolder("TestFolder")

        assertThat(folder.serverId).isEqualTo("TestFolder")
    }

    @Test(expected = MessagingException::class)
    fun `checkSettings() with TrustedSocketFactory throwing should throw MessagingException`() {
        stubbing(trustedSocketFactory) {
            on { createSocket(null, HOST, 12345, null) } doThrow IOException()
        }

        store.checkSettings()
    }

    @Test(expected = MessagingException::class)
    fun `checkSettings() with UIDL command not supported should throw MessagingException`() {
        setupSocketWithResponse(
            INITIAL_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE +
                UIDL_UNSUPPORTED_RESPONSE,
        )

        store.checkSettings()
    }

    @Test
    fun `checkSettings() with UIDL supported`() {
        setupSocketWithResponse(
            INITIAL_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE +
                UIDL_SUPPORTED_RESPONSE,
        )

        store.checkSettings()
    }

    private fun createServerSettings(): ServerSettings {
        return ServerSettings(
            type = "pop3",
            host = HOST,
            port = 12345,
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED,
            authenticationType = AuthType.PLAIN,
            username = "user",
            password = "password",
            clientCertificateAlias = null,
        )
    }

    private fun setupSocketWithResponse(response: String): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()

        val socket = mock<Socket> {
            on { isConnected } doReturn true
            on { isClosed } doReturn false
            on { getOutputStream() } doReturn outputStream
            on { getInputStream() } doReturn response.byteInputStream()
        }

        stubbing(trustedSocketFactory) {
            on { createSocket(null, HOST, 12345, null) } doReturn socket
        }

        return outputStream
    }

    companion object {
        private const val HOST = "127.0.0.1"
        private const val INITIAL_RESPONSE = "+OK POP3 server greeting\r\n"

        private const val CAPA_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "SASL PLAIN CRAM-MD5 EXTERNAL\r\n" +
            ".\r\n"

        private const val AUTH_PLAIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n"

        private const val STAT_RESPONSE = "+OK 20 0\r\n"

        private const val UIDL_UNSUPPORTED_RESPONSE = "-ERR UIDL unsupported\r\n"
        private const val UIDL_SUPPORTED_RESPONSE = "+OK UIDL supported\r\n"
    }
}
