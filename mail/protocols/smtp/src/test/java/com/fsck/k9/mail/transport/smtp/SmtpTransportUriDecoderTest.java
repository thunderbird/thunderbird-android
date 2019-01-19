package com.fsck.k9.mail.transport.smtp;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


public class SmtpTransportUriDecoderTest {
    @Test
    public void decodeTransportUri_canDecodeAuthType() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodePassword() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("password", result.password);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername_withNoAuthType() {
        String storeUri = "smtp://user:password@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername_withNoPasswordOrAuthType() {
        String storeUri = "smtp://user@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodeAuthType_withEmptyPassword() {
        String storeUri = "smtp://user::PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeTransportUri_canDecodeHost() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("server", result.host);
    }

    @Test
    public void decodeTransportUri_canDecodePort() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals(123456, result.port);
    }

    @Test
    public void decodeTransportUri_canDecodeTLS() {
        String storeUri = "smtp+tls+://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeTransportUri_canDecodeSSL() {
        String storeUri = "smtp+ssl+://user:password:PLAIN@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeTransportUri_canDecodeClientCert() {
        String storeUri = "smtp+ssl+://user:clientCert:EXTERNAL@server:123456";

        ServerSettings result = SmtpTransportUriDecoder.decodeSmtpUri(storeUri);

        assertEquals("clientCert", result.clientCertificateAlias);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeTransportUri_forUnknownSchema_throwsIllegalArgumentException() {
        String storeUri = "unknown://user:clientCert:EXTERNAL@server:123456";

        SmtpTransportUriDecoder.decodeSmtpUri(storeUri);
    }
}
