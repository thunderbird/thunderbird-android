package com.fsck.k9.mail.store.imap;

import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.RemoteStore;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ImapStoreUriTest  {
    @Test public void testDecodeStoreUriImapAllExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix";
        ServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test public void testDecodeStoreUriImapNoExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/";
        ServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
    }

    @Test public void testDecodeStoreUriImapPrefixOnly() {
        String uri = "imap://PLAIN:user:pass@server:143/customPathPrefix";
        ServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    @Test public void testDecodeStoreUriImapEmptyPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7C";
        ServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("", settings.getExtra().get("pathPrefix"));
    }

    @Test public void testDecodeStoreUriImapAutodetectAndPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/1%7CcustomPathPrefix";
        ServerSettings settings = RemoteStore.decodeStoreUri(uri);

        assertEquals(AuthType.PLAIN, settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
        assertNull(settings.getExtra().get("pathPrefix"));
    }


    @Test public void testCreateStoreUriImapPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "customPathPrefix");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix", uri);
    }

    @Test public void testCreateStoreUriImapEmptyPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7C", uri);
    }

    @Test public void testCreateStoreUriImapNoExtra() {
        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }

    @Test public void testCreateStoreUriImapAutoDetectNamespace() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "true");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, AuthType.PLAIN, "user", "pass", null, extra);

        String uri = RemoteStore.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }
}
