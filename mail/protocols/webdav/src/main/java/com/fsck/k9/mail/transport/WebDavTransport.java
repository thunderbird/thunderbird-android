package com.fsck.k9.mail.transport;


import java.util.Collections;

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.ssl.TrustManagerFactory;
import com.fsck.k9.mail.store.webdav.DraftsFolderProvider;
import com.fsck.k9.mail.store.webdav.SniHostSetter;
import com.fsck.k9.mail.store.webdav.WebDavStore;

public class WebDavTransport extends Transport {
    private WebDavStore store;

    public WebDavTransport(TrustManagerFactory trustManagerFactory, SniHostSetter sniHostSetter,
            ServerSettings serverSettings, DraftsFolderProvider draftsFolderProvider) {
        store = new WebDavStore(trustManagerFactory, sniHostSetter, serverSettings, draftsFolderProvider);

        if (K9MailLib.isDebug())
            Timber.d(">>> New WebDavTransport creation complete");
    }

    @Override
    public void open() throws MessagingException {
        if (K9MailLib.isDebug())
            Timber.d( ">>> open called on WebDavTransport ");

        store.getHttpClient();
    }

    @Override
    public void close() {
    }

    @Override
    public void sendMessage(Message message) throws MessagingException {
        store.sendMessages(Collections.singletonList(message));
    }

    public void checkSettings() throws MessagingException {
        store.checkSettings();
    }
}
