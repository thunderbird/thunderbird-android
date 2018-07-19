package com.fsck.k9.backend.webdav;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class WebDavStoreUriCreatorTest {
    @Test
    public void createUri_withSetting_shouldProvideUri() {
        ServerSettings serverSettings = new ServerSettings("webdav", "example.org", 123456, ConnectionSecurity.NONE,
                AuthType.PLAIN, "user", "password", null);

        String result = WebDavStoreUriCreator.create(serverSettings);

        assertEquals("webdav://user:password@example.org:123456/%7C%7C", result);
    }

    @Test
    public void createUri_withSettingsWithTLS_shouldProvideSSLUri() {
        ServerSettings serverSettings = new ServerSettings("webdav", "example.org", 123456,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, "user", "password", null);

        String result = WebDavStoreUriCreator.create(serverSettings);

        assertEquals("webdav+ssl+://user:password@example.org:123456/%7C%7C", result);
    }
}
