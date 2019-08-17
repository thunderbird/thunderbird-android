package com.fsck.k9.backend.eas

import com.fsck.k9.mail.ConnectionSecurity
import org.junit.Assert.assertEquals
import org.junit.Test

class EasServerSettingsTest {

    @Test
    fun createUri_withSSLTLS_required_shouldProduceSSLUri() {
        val serverSettings = EasServerSettings(
                "hotmail.com",
                8443,
                ConnectionSecurity.SSL_TLS_REQUIRED,
                "test@hotmail.com",
                "passö$")

        val uri = EasServerSettings.encode(serverSettings)

        assertEquals(uri, "eas+https://test%2540hotmail.com:pass%25C3%25B6%2524@hotmail.com:8443/")
    }

    @Test
    fun createUri_withNONE_required_shouldProduceSSLUri() {
        val serverSettings = EasServerSettings(
                "hotmail.com",
                80,
                ConnectionSecurity.NONE,
                "test@hotmail.com",
                "passö$")

        val uri = EasServerSettings.encode(serverSettings)

        assertEquals(uri, "eas+http://test%2540hotmail.com:pass%25C3%25B6%2524@hotmail.com/")
    }

    @Test(expected = IllegalArgumentException::class)
    fun createUri_withSTARTTLS_required_shouldProduceSSLUri() {
        val serverSettings = EasServerSettings(
                "hotmail.com",
                80,
                ConnectionSecurity.STARTTLS_REQUIRED,
                "test@hotmail.com",
                "passö$")

        EasServerSettings.encode(serverSettings)
    }

    @Test
    fun decodeUri_withSSLTLSUri_shouldUseStartTls() {
        val settings = EasServerSettings.decode("eas+https://test%2540hotmail.com:pass%25C3%25B6%2524@hotmail.com/")

        assertEquals(settings.type, "eas")
        assertEquals(settings.connectionSecurity, ConnectionSecurity.SSL_TLS_REQUIRED)
        assertEquals(settings.host, "hotmail.com")
        assertEquals(settings.port, 443)
        assertEquals(settings.username, "test@hotmail.com")
        assertEquals(settings.password, "passö\$")
    }

    @Test
    fun decodeUri_withNONEUri_shouldUseStartTls() {
        val settings = EasServerSettings.decode("eas+http://test%2540hotmail.com:pass%25C3%25B6%2524@hotmail.com/")

        assertEquals(settings.type, "eas")
        assertEquals(settings.connectionSecurity, ConnectionSecurity.NONE)
        assertEquals(settings.host, "hotmail.com")
        assertEquals(settings.port, 80)
        assertEquals(settings.username, "test@hotmail.com")
        assertEquals(settings.password, "passö\$")
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeUri_withEASUri_shouldThrowException() {
        EasServerSettings.decode("imap://PLAIN:user:password@server:12345")
    }
}
