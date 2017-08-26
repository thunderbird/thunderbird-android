package com.fsck.k9.mail;


import android.annotation.SuppressLint;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;


@SuppressLint("AuthLeak")
public class TransportUrisTest {

    @Test
    public void encodeThenDecode() throws Exception {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.CRAM_MD5,
                "user", "password", null);

        String uri = TransportUris.createTransportUri(serverSettings);
        ServerSettings decodedSettings = TransportUris.decodeTransportUri(uri);

        assertEquals(serverSettings, decodedSettings);
    }

    @Test
    public void encodeThenDecode_externalAuth_preservesCert() throws Exception {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.NONE, AuthType.EXTERNAL,
                "username", null, "clientcert");

        String uri = TransportUris.createTransportUri(serverSettings);
        ServerSettings decodedSettings = TransportUris.decodeTransportUri(uri);

        assertEquals(serverSettings, decodedSettings);
    }

    @Test
    public void decodeTransportUri_canDecodeAuthType() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodePassword() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("password", result.password);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername_withNoAuthType() {
        String storeUri = "smtp://user:password@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodeUsername_withNoPasswordOrAuthType() {
        String storeUri = "smtp://user@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("user", result.username);
    }

    @Test
    public void decodeTransportUri_canDecodeAuthType_withEmptyPassword() {
        String storeUri = "smtp://user::PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals(AuthType.PLAIN, result.authenticationType);
    }

    @Test
    public void decodeTransportUri_canDecodeHost() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("server", result.host);
    }

    @Test
    public void decodeTransportUri_canDecodePort() {
        String storeUri = "smtp://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals(123456, result.port);
    }

    @Test
    public void decodeTransportUri_canDecodeTLS() {
        String storeUri = "smtp+tls+://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeTransportUri_canDecodeSSL() {
        String storeUri = "smtp+ssl+://user:password:PLAIN@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, result.connectionSecurity);
    }

    @Test
    public void decodeTransportUri_canDecodeClientCert() {
        String storeUri = "smtp+ssl+://user:clientCert:EXTERNAL@server:123456";

        ServerSettings result = TransportUris.decodeTransportUri(storeUri);

        assertEquals("clientCert", result.clientCertificateAlias);
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeTransportUri_forUnknownSchema_throwsIllegalArgumentException() {
        String storeUri = "unknown://user:clientCert:EXTERNAL@server:123456";

        TransportUris.decodeTransportUri(storeUri);
    }

    @Test
    public void createTransportUri_canEncodeSmtpSslUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.EXTERNAL,
                "user", "password", "clientCert");

        String result = TransportUris.createTransportUri(serverSettings);

        assertEquals("smtp+ssl+://user:clientCert:EXTERNAL@server:123456", result);
    }

    @Test
    public void createTransportUri_canEncodeSmtpTlsUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.STARTTLS_REQUIRED, AuthType.PLAIN,
                "user", "password", "clientCert");

        String result = TransportUris.createTransportUri(serverSettings);

        assertEquals("smtp+tls+://user:password:PLAIN@server:123456", result);
    }

    @Test
    public void createTransportUri_canEncodeSmtpUri() {
        ServerSettings serverSettings = new ServerSettings(
                ServerSettings.Type.SMTP, "server", 123456,
                ConnectionSecurity.NONE, AuthType.CRAM_MD5,
                "user", "password", "clientCert");

        String result = TransportUris.createTransportUri(serverSettings);

        assertEquals("smtp://user:password:CRAM_MD5@server:123456", result);
    }
}