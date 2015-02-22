
package com.fsck.k9.mail.transport;

import android.util.Log;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.store.webdav.WebDavStore;

import java.util.Collections;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;

public class WebDavTransport extends Transport {

    /**
     * Decodes a WebDavTransport URI.
     *
     * <p>
     * <b>Note:</b> Everything related to sending messages via WebDAV is handled by
     * {@link WebDavStore}. So the transport URI is the same as the store URI.
     * </p>
     */
    public static ServerSettings decodeUri(String uri) {
        return WebDavStore.decodeUri(uri);
    }

    /**
     * Creates a WebDavTransport URI.
     *
     * <p>
     * <b>Note:</b> Everything related to sending messages via WebDAV is handled by
     * {@link WebDavStore}. So the transport URI is the same as the store URI.
     * </p>
     */
    public static String createUri(ServerSettings server) {
        return WebDavStore.createUri(server);
    }


    private WebDavStore store;

    public WebDavTransport(StoreConfig storeConfig) throws MessagingException {
        store = new WebDavStore(storeConfig);

        if (K9MailLib.isDebug())
            Log.d(LOG_TAG, ">>> New WebDavTransport creation complete");
    }

    @Override
    public void open() throws MessagingException {
        if (K9MailLib.isDebug())
            Log.d(LOG_TAG, ">>> open called on WebDavTransport ");

        store.getHttpClient();
    }

    @Override
    public void close() {
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        store.sendMessages(Collections.singletonList(message));
    }
}
