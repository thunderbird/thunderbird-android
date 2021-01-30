package com.fsck.k9.backend.imap;


import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class ImapStoreUriTest {
    @Test
    public void testDecodeStoreUriImapNoAuth() {
        String uri = "imap://user:pass@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapNoPassword() {
        String uri = "imap://user:@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapPlainNoPassword() {
        String uri = "imap://PLAIN:user:@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapExternalAuth() {
        String uri = "imap://EXTERNAL:user:clientCertAlias@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.EXTERNAL, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals("clientCertAlias", settings.clientCertificateAlias);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapXOAuth2() {
        String uri = "imap://XOAUTH2:user:@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.XOAUTH2, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals(null, settings.password);
        assertEquals(null, settings.clientCertificateAlias);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapSSL() {
        String uri = "imap+tls+://PLAIN:user:pass@server/";
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(ConnectionSecurity.STARTTLS_REQUIRED, settings.connectionSecurity);
        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapTLS() {
        String uri = "imap+ssl+://PLAIN:user:pass@server/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(ConnectionSecurity.SSL_TLS_REQUIRED, settings.connectionSecurity);
        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
    }

    @Test
    public void testDecodeStoreUriImapAllExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapNoExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
    }

    @Test
    public void testDecodeStoreUriImapPrefixOnly() {
        String uri = "imap://PLAIN:user:pass@server:143/customPathPrefix";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapEmptyPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7C";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapAutodetectAndPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/1%7CcustomPathPrefix";
        
        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
        assertNull(settings.getExtra().get("pathPrefix"));
    }


    @Test
    public void testDecodeStoreUriImapHybridPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix?prefix=%2FcustomPathPrefix&auth-type=plain";

        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }


    @Test
    public void testDecodeStoreUriImapCleanishPrefix() {
        String uri = "imap://user:pass@server?prefix=/customPathPrefix&auth-type=plain";

        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }


    @Test
    public void testDecodeStoreUriImapCleanishMixExternal() {
        String uri = "imap+ssl+://EXTERNAL:user:emailCert@server/1?prefix=/customPathPrefix&auth-type=external";

        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.EXTERNAL, settings.authenticationType);
        assertEquals("user", settings.username);
        assertNull(settings.password);
        assertEquals("emailCert", settings.clientCertificateAlias);
        assertEquals("server", settings.host);
        assertEquals(993, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapCleanishPureExternal() {
        String uri = "imap+tls+://user@server:1993/?tls-cert=emailCert&prefix=Auto&auth-type=external";

        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals(AuthType.EXTERNAL, settings.authenticationType);
        assertEquals("user", settings.username);
        assertNull(settings.password);
        assertEquals("emailCert", settings.clientCertificateAlias);
        assertEquals("server", settings.host);
        assertEquals(1993, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
        assertNull(settings.getExtra().get("pathPrefix"));
    }

    @Test
    public void testDecodeStoreUriImapFunkyPrefixl() {
        String uri = "imap+ssl+://PLAIN:user:pass@server:993?prefix=%2FThis%22Is%3F%3DNot%26+%23Healthy%2B&tls-cert=emailCert&auth-type=plain";

        ServerSettings settings = ImapStoreUriDecoder.decode(uri);

        assertEquals("This\"Is?=Not& #Healthy+", settings.getExtra().get("pathPrefix"));


    }



    @Test
    public void testCreateStoreUriImapPrefix() {
        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "customPathPrefix");
        ServerSettings settings = new ServerSettings("imap", "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix?prefix=%2FcustomPathPrefix&auth-type=plain", uri);
    }

    @Test
    public void testCreateStoreUriImapExternal() {
        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "");
        ServerSettings settings = new ServerSettings("imap", "server", 993,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.EXTERNAL, "user", "pass", "emailCert", extra);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap+ssl+://EXTERNAL:user:emailCert@server:993/0%7C?prefix=%2F&tls-cert=emailCert&auth-type=external", uri);
    }


    @Test
    public void testCreateStoreUriImapFunkyPrefixl() {
        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "This\"Is?=Not& #Healthy+");
        ServerSettings settings = new ServerSettings("imap", "server", 993,
                ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, "user", "pass", "emailCert", extra);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap+ssl+://PLAIN:user:pass@server:993/0%7CThis%22Is%3F=Not&%20%23Healthy+?prefix=%2FThis%22Is%3F%3DNot%26+%23Healthy%2B&tls-cert=emailCert&auth-type=plain", uri);
    }

    @Test
    public void testCreateStoreUriImapEmptyPrefix() {
        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "");
        ServerSettings settings = new ServerSettings("imap", "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7C?prefix=%2F&auth-type=plain", uri);
    }

    @Test
    public void testCreateStoreUriImapNoExtra() {
        ServerSettings settings = new ServerSettings("imap", "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C?prefix=auto&auth-type=plain", uri);
    }

    @Test
    public void testCreateStoreUriImapAutoDetectNamespace() {
        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", "true");

        ServerSettings settings = new ServerSettings("imap", "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C?prefix=auto&auth-type=plain", uri);
    }

    @Test
    public void testCreateDecodeStoreUriWithSpecialCharactersInUsernameAndPassword() {
        ServerSettings settings = new ServerSettings("imap", "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user@doma:n", "p@ssw:rd%", null, null);

        String uri = ImapStoreUriCreator.create(settings);

        assertEquals("imap://PLAIN:user%2540doma%253An:p%2540ssw%253Ard%2525@server:143/1%7C?prefix=auto&auth-type=plain", uri);

        ServerSettings outSettings = ImapStoreUriDecoder.decode(uri);

        assertEquals("user@doma:n", outSettings.username);
        assertEquals("p@ssw:rd%", outSettings.password);
    }
}
