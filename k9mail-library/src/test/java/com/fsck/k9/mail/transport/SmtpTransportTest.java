package com.fsck.k9.mail.transport;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SmtpTransportTest {

    @Test
    public void decodeUri_canDecodeAuthType() {
        assertEquals(AuthType.PLAIN,
                SmtpTransport.decodeUri("smtp://user:password:PLAIN@server:123456")
                .authenticationType);
    }

    @Test
    public void decodeUri_canDecodeUsername() {
        assertEquals("user",
                SmtpTransport.decodeUri("smtp://user:password:PLAIN@server:123456")
                        .username);
    }

    @Test
    public void decodeUri_canDecodePassword() {
        assertEquals("password",
                SmtpTransport.decodeUri("smtp://user:password:PLAIN@server:123456")
                        .password);
    }

    @Test
    public void decodeUri_canDecodeHost() {
        assertEquals("server",
                SmtpTransport.decodeUri("smtp://user:password:PLAIN@server:123456")
                        .host);
    }

    @Test
    public void decodeUri_canDecodePort() {
        assertEquals(123456,
                SmtpTransport.decodeUri("smtp://user:password:PLAIN@server:123456")
                        .port);
    }

    @Test
    public void decodeUri_canDecodeTLS() {
        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED,
                SmtpTransport.decodeUri("smtp+tls+://user:password:PLAIN@server:123456")
                        .connectionSecurity);
    }

    @Test
    public void decodeUri_canDecodeSSL() {
        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED,
                SmtpTransport.decodeUri("smtp+ssl+://user:password:PLAIN@server:123456")
                        .connectionSecurity);
    }

    @Test
    public void decodeUri_canDecodeClientCert() {
        assertEquals("clientCert",
                SmtpTransport.decodeUri("smtp+ssl+://user:clientCert:EXTERNAL@server:123456")
                        .clientCertificateAlias);
    }

    @Test
    public void createUri_canEncodeSmtpSslUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.EXTERNAL,
                "user", "password", "clientCert");
        assertEquals("smtp+ssl+://user:clientCert:EXTERNAL@server:123456",
                SmtpTransport.createUri(serverSettings));
    }

    @Test
    public void createUri_canEncodeSmtpTlsUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.PLAIN,
                "user", "password", "clientCert");
        assertEquals("smtp+tls+://user:password:PLAIN@server:123456",
                SmtpTransport.createUri(serverSettings));
    }

    @Test
    public void createUri_canEncodeSmtpUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.NONE, AuthType.CRAM_MD5,
                "user", "password", "clientCert");
        assertEquals("smtp://user:password:CRAM_MD5@server:123456",
                SmtpTransport.createUri(serverSettings));
    }
}
