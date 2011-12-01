package com.fsck.k9.mail.store;

import java.util.HashMap;
import java.util.Map;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.store.ImapStore;
import junit.framework.TestCase;

public class ImapStoreUriTest extends TestCase {
    public void testDecodeStoreUriImapAllExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix";
        ServerSettings settings = Store.decodeStoreUri(uri);

        assertEquals("PLAIN", settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    public void testDecodeStoreUriImapNoExtras() {
        String uri = "imap://PLAIN:user:pass@server:143/";
        ServerSettings settings = Store.decodeStoreUri(uri);

        assertEquals("PLAIN", settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
    }

    public void testDecodeStoreUriImapPrefixOnly() {
        String uri = "imap://PLAIN:user:pass@server:143/customPathPrefix";
        ServerSettings settings = Store.decodeStoreUri(uri);

        assertEquals("PLAIN", settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("customPathPrefix", settings.getExtra().get("pathPrefix"));
    }

    public void testDecodeStoreUriImapEmptyPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/0%7C";
        ServerSettings settings = Store.decodeStoreUri(uri);

        assertEquals("PLAIN", settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("false", settings.getExtra().get("autoDetectNamespace"));
        assertEquals("", settings.getExtra().get("pathPrefix"));
    }

    public void testDecodeStoreUriImapAutodetectAndPrefix() {
        String uri = "imap://PLAIN:user:pass@server:143/1%7CcustomPathPrefix";
        ServerSettings settings = Store.decodeStoreUri(uri);

        assertEquals("PLAIN", settings.authenticationType);
        assertEquals("user", settings.username);
        assertEquals("pass", settings.password);
        assertEquals("server", settings.host);
        assertEquals(143, settings.port);
        assertEquals("true", settings.getExtra().get("autoDetectNamespace"));
        assertNull(settings.getExtra().get("pathPrefix"));
    }


    public void testCreateStoreUriImapPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "customPathPrefix");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, "PLAIN", "user", "pass", extra);

        String uri = Store.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7CcustomPathPrefix", uri);
    }

    public void testCreateStoreUriImapEmptyPrefix() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "false");
        extra.put("pathPrefix", "");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, "PLAIN", "user", "pass", extra);

        String uri = Store.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/0%7C", uri);
    }

    public void testCreateStoreUriImapNoExtra() {
        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, "PLAIN", "user", "pass");

        String uri = Store.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }

    public void testCreateStoreUriImapAutoDetectNamespace() {
        Map<String, String> extra = new HashMap<String, String>();
        extra.put("autoDetectNamespace", "true");

        ServerSettings settings = new ServerSettings(ImapStore.STORE_TYPE, "server", 143,
                ConnectionSecurity.NONE, "PLAIN", "user", "pass", extra);

        String uri = Store.createStoreUri(settings);

        assertEquals("imap://PLAIN:user:pass@server:143/1%7C", uri);
    }
}
