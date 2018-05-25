package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.StoreConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;

public class WebDavStoreUriCreator {

    /**
     * Creates a WebDavStore URI with the supplied settings.
     *
     * @param server The {@link ServerSettings} object that holds the server settings.
     * @return A WebDavStore URI that holds the same information as the {@code server} parameter.
     * @see StoreConfig#getStoreUri()
     * @see WebDavStore#decodeUri(String)
     */
    public static String create(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "webdav+ssl+";
                break;
            default:
            case NONE:
                scheme = "webdav";
                break;
        }

        String userInfo = userEnc + ":" + passwordEnc;

        String uriPath;
        Map<String, String> extra = server.getExtra();
        if (extra != null) {
            String path = extra.get(WebDavStoreSettings.PATH_KEY);
            path = (path != null) ? path : "";
            String authPath = extra.get(WebDavStoreSettings.AUTH_PATH_KEY);
            authPath = (authPath != null) ? authPath : "";
            String mailboxPath = extra.get(WebDavStoreSettings.MAILBOX_PATH_KEY);
            mailboxPath = (mailboxPath != null) ? mailboxPath : "";
            uriPath = "/" + path + "|" + authPath + "|" + mailboxPath;
        } else {
            uriPath = "/||";
        }

        try {
            return new URI(scheme, userInfo, server.host, server.port, uriPath,
                    null, null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create WebDavStore URI", e);
        }
    }
}
