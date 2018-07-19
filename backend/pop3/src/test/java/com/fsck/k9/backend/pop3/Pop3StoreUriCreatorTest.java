package com.fsck.k9.backend.pop3;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class Pop3StoreUriCreatorTest {

    @Test
    public void createUri_withSSLTLS_required_shouldProduceSSLUri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3+ssl+://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withSTARTTLSRequired_shouldProduceTLSUri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.STARTTLS_REQUIRED,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3+tls+://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withNONE_shouldProducePop3Uri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.NONE,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withPLAIN_shouldProducePlainAuthUri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.NONE,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withEXTERNAL_shouldProduceExternalAuthUri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.NONE,
                AuthType.EXTERNAL, "user", "password", "clientCert");

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3://EXTERNAL:user:clientCert@server:12345");
    }

    @Test
    public void createUri_withCRAMMD5_shouldProduceCRAMMD5AuthUri() {
        ServerSettings settings = new ServerSettings("pop3", "server", 12345, ConnectionSecurity.NONE,
                AuthType.CRAM_MD5, "user", "password", "clientCert");

        String uri = Pop3StoreUriCreator.create(settings);

        assertEquals(uri, "pop3://CRAM_MD5:user:password@server:12345");
    }
}
