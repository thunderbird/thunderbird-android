package com.fsck.k9.mail.transport;

import android.annotation.SuppressLint;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressLint("AuthLeak")
public class SmtpTransportUriTest {

    @Test
    public void decodeUri_canDecodeAuthType() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeUri_canDecodeUsername() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeUri_canDecodePassword() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("password", result.password);
    }

    @Test
    public void decodeUri_canDecodeUsername_withNoAuthType() {
        String storeUri = "smtp://user:password@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeUri_canDecodeUsername_withNoPasswordOrAuthType() {
        String storeUri = "smtp://user@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeUri_canDecodeAuthType_withEmptyPassword() {
        String storeUri = "smtp://user::PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeUri_canDecodeHost() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("server", result.host);
    }

    @Test
    public void decodeUri_canDecodePort() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals(123456, result.port);
    }

    @Test
    public void decodeUri_canDecodeTLS() {
        String storeUri = "smtp+tls+://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeUri_canDecodeSSL() {
        String storeUri = "smtp+ssl+://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeUri_canDecodeClientCert() {
        String storeUri = "smtp+ssl+://user:clientCert:EXTERNAL@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);

        assertEquals("clientCert", result.clientCertificateAlias);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeUri_forUnknownSchema_throwsIllegalArgumentException() {
        String storeUri = "unknown://user:clientCert:EXTERNAL@server:123456";

        ServerSettings result = SmtpTransport.decodeUri(storeUri);
    }

    @Test
    public void createUri_canEncodeSmtpSslUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.EXTERNAL,
                "user", "password", "clientCert");

        String result = SmtpTransport.createUri(serverSettings);

        assertEquals("smtp+ssl+://user:clientCert:EXTERNAL@server:123456", result);
    }

    @Test
    public void createUri_canEncodeSmtpTlsUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.PLAIN,
                "user", "password", "clientCert");

        String result = SmtpTransport.createUri(serverSettings);

        assertEquals("smtp+tls+://user:password:PLAIN@server:123456", result);
    }

    @Test
    public void createUri_canEncodeSmtpUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.NONE, AuthType.CRAM_MD5,
                "user", "password", "clientCert");

        String result = SmtpTransport.createUri(serverSettings);

        assertEquals("smtp://user:password:CRAM_MD5@server:123456", result);
    }
}
