package com.fsck.k9.backend.pop3;


import java.net.URI;
import java.net.URISyntaxException;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ServerSettings;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;


public class Pop3StoreUriCreator {
    /**
     * Creates a Pop3Store URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A Pop3Store URI that holds the same information as the {@code server} parameter.
     */
    public static String create(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "pop3+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "pop3+tls+";
                break;
            default:
            case NONE:
                scheme = "pop3";
                break;
        }

        AuthType authType = server.authenticationType;
        String userInfo;
        if (AuthType.EXTERNAL == authType) {
            userInfo = authType.name() + ":" + userEnc + ":" + clientCertificateAliasEnc;
        } else {
            userInfo = authType.name() + ":" + userEnc + ":" + passwordEnc;
        }

        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create Pop3Store URI", e);
        }
    }
}
