package com.fsck.k9.mail.transport.smtp;


import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ServerSettings;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.buildQuery;


public class SmtpTransportUriCreator {

    /**
     * Creates a SmtpTransport URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A SmtpTransport URI that holds the same information as the {@code server} parameter.
     */
    public static String createSmtpUri(ServerSettings server) {
        String userEnc = (server.username != null) ?
                encodeUtf8(server.username) : "";
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";


        HashMap<String,String> params = new HashMap<>();

        // this has a little duplicate logic w.r.t. the legacy uri encoding below, , but it's better that way
        //  so we can remove the legacy format in the future

        if (! clientCertificateAliasEnc.equals("") ) {
            params.put("tls-cert",clientCertificateAliasEnc);
        }
        if (server.authenticationType != null) {
            params.put("auth-type", server.authenticationType.name().toLowerCase());
        }
        String query = buildQuery(params);



        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "smtp+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "smtp+tls+";
                break;
            default:
            case NONE:
                scheme = "smtp";
                break;
        }

        String userInfo;
        AuthType authType = server.authenticationType;
        // NOTE: authType is append at last item, in contrast to ImapStore and Pop3Store!
        if (authType != null) {
            if (AuthType.EXTERNAL == authType) {
                userInfo = userEnc + ":" + clientCertificateAliasEnc + ":" + authType.name();
            } else {
                userInfo = userEnc + ":" + passwordEnc + ":" + authType.name();
            }
        } else {
            userInfo = userEnc + ":" + passwordEnc;
            if (userInfo.equals(":")) {
                userInfo = null;
            } else if ((passwordEnc == null) || (passwordEnc.equals(""))) {
                userInfo = userEnc;
            }
        }
        try {
            return new URI(scheme, userInfo, server.host, server.port, null, query,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create SmtpTransport URI", e);
        }
    }

    private static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
