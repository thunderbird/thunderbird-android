package com.fsck.k9.backend.imap;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.imap.ImapStoreSettings;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.buildQuery;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;


public class ImapStoreUriCreator {
    /**
     * Creates an ImapStore URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return An ImapStore URI that holds the same information as the {@code server} parameter.
     */
    public static String create(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ? encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";

        HashMap<String,String> params = new HashMap<>();

        // this has a little duplicate logic w.r.t. the legacy uri encoding below, , but it's better that way
        //  so we can remove the legacy format in the future

        if (! clientCertificateAliasEnc.equals("") ) {
            params.put("tls-cert",clientCertificateAliasEnc);
        }
        params.put("auth-type",server.authenticationType.name().toLowerCase());
        if ((server.getExtra() == null) ||
                ( server.getExtra().get(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY).equals("true") )) {
            params.put("prefix","auto");
        } else if (( server.getExtra().get(ImapStoreSettings.PATH_PREFIX_KEY) != null) &&
                (! server.getExtra().get(ImapStoreSettings.PATH_PREFIX_KEY).equals("") ) ) {
            params.put("prefix", "/" + server.getExtra().get(ImapStoreSettings.PATH_PREFIX_KEY) );
        } else {
            params.put("prefix", "/");
        }

        String query = buildQuery(params);

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "imap+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "imap+tls+";
                break;
            default:
            case NONE:
                scheme = "imap";
                break;
        }

        AuthType authType = server.authenticationType;
        String userInfo;
        if (authType == AuthType.EXTERNAL) {
            userInfo = authType.name() + ":" + userEnc + ":" + clientCertificateAliasEnc;
        } else {
            userInfo = authType.name() + ":" + userEnc + ":" + passwordEnc;
        }
        try {
            Map<String, String> extra = server.getExtra();
            String path;
            if (extra != null) {
                boolean autoDetectNamespace = Boolean.TRUE.toString().equals(
                        extra.get(ImapStoreSettings.AUTODETECT_NAMESPACE_KEY));
                String pathPrefix = (autoDetectNamespace) ?
                        null : extra.get(ImapStoreSettings.PATH_PREFIX_KEY);
                path = "/" + (autoDetectNamespace ? "1" : "0") + "|" +
                        ((pathPrefix == null) ? "" : pathPrefix);
            } else {
                path = "/1|";
            }
            // query is assumed to have at least one entry, and we have to do the encoding ourselves, due to limitations in URI
            return new URI(scheme, userInfo, server.host, server.port, path, null, null).toString() + "?" + query;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create ImapStore URI", e);
        }
    }
}
