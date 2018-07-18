package com.fsck.k9.mail.transport.smtp;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


public class SmtpTransportUriCreatorTest {
    @Test
    public void encodeThenDecode() {
        ServerSettings serverSettings = new ServerSettings(
                "smtp", "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.CRAM_MD5,
                "user", "password", null);

        String uri = SmtpTransportUriCreator.createSmtpUri(serverSettings);
        ServerSettings decodedSettings = SmtpTransportUriDecoder.decodeSmtpUri(uri);

        assertEquals(serverSettings, decodedSettings);
    }

    @Test
    public void encodeThenDecode_externalAuth_preservesCert() {
        ServerSettings serverSettings = new ServerSettings(
                "smtp", "server", 123456,
                ConnectionSecurity.NONE, AuthType.EXTERNAL,
                "username", null, "clientcert");

        String uri = SmtpTransportUriCreator.createSmtpUri(serverSettings);
        ServerSettings decodedSettings = SmtpTransportUriDecoder.decodeSmtpUri(uri);

        assertEquals(serverSettings, decodedSettings);
    }


    @Test
    public void createTransportUri_canEncodeSmtpSslUri() {
        ServerSettings serverSettings = new ServerSettings(
                "smtp", "server", 123456,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.EXTERNAL,
                "user", "password", "clientCert");

        String result = SmtpTransportUriCreator.createSmtpUri(serverSettings);

        assertEquals("smtp+ssl+://user:clientCert:EXTERNAL@server:123456", result);
    }

    @Test
    public void createTransportUri_canEncodeSmtpTlsUri() {
        ServerSettings serverSettings = new ServerSettings(
                "smtp", "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.PLAIN,
                "user", "password", "clientCert");

        String result = SmtpTransportUriCreator.createSmtpUri(serverSettings);

        assertEquals("smtp+tls+://user:password:PLAIN@server:123456", result);
    }

    @Test
    public void createTransportUri_canEncodeSmtpUri() {
        ServerSettings serverSettings = new ServerSettings(
                "smtp", "server", 123456,
                ConnectionSecurity.NONE, AuthType.CRAM_MD5,
                "user", "password", "clientCert");

        String result = SmtpTransportUriCreator.createSmtpUri(serverSettings);

        assertEquals("smtp://user:password:CRAM_MD5@server:123456", result);
    }
}
