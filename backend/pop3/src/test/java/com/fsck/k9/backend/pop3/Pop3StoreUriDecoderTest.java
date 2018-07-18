package com.fsck.k9.backend.pop3;


import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Pop3StoreUriDecoderTest {
    @Test
    public void decodeUri_withTLSUri_shouldUseStartTls() {
        ServerSettings settings = Pop3StoreUriDecoder.decode("pop3+tls+://PLAIN:user:password@server:12345");

        assertEquals(settings.connectionSecurity, ConnectionSecurity.STARTTLS_REQUIRED);
    }

    @Test
    public void decodeUri_withPlainUri_shouldUseNoSecurity() {
        ServerSettings settings = Pop3StoreUriDecoder.decode("pop3://PLAIN:user:password@server:12345");
        assertEquals(settings.connectionSecurity, ConnectionSecurity.NONE);
    }

    @Test
    public void decodeUri_withExternalCertificateShouldProvideAlias_shouldUseNoSecurity() {
        ServerSettings settings = Pop3StoreUriDecoder.decode("pop3://EXTERNAL:user:clientCert@server:12345");

        assertEquals(settings.clientCertificateAlias, "clientCert");
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeUri_withNonPop3Uri_shouldThrowException() {
        Pop3StoreUriDecoder.decode("imap://PLAIN:user:password@server:12345");
    }
}
