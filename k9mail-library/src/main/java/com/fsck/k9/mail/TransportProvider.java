package com.fsck.k9.mail;


import android.content.Context;

import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.transport.smtp.SmtpTransport;
import com.fsck.k9.mail.transport.WebDavTransport;

public class TransportProvider {
    private static TransportProvider transportProvider = new TransportProvider();

    public static TransportProvider getInstance() {
        return transportProvider;
    }

    public synchronized Transport getTransport(Context context, StoreConfig storeConfig)
            throws MessagingException {
        String uri = storeConfig.getTransportUri();
        if (uri.startsWith("smtp")) {
            OAuth2TokenProvider oauth2TokenProvider = null;
            return new SmtpTransport(storeConfig, new DefaultTrustedSocketFactory(context), oauth2TokenProvider);
        } else if (uri.startsWith("webdav")) {
            return new WebDavTransport(storeConfig);
        } else {
            throw new MessagingException("Unable to locate an applicable Transport for " + uri);
        }
    }
}
