package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;

/**
 * This class is used to store the decoded contents of an WebDavStore URI.
 *
 * @see WebDavStore#decodeUri(String)
 */
public class WebDavStoreSettings extends ServerSettings {
    public static final String ALIAS_KEY = "alias";
    public static final String PATH_KEY = "path";
    public static final String AUTH_PATH_KEY = "authPath";
    public static final String MAILBOX_PATH_KEY = "mailboxPath";

    public final String alias;
    public final String path;
    public final String authPath;
    public final String mailboxPath;

    protected WebDavStoreSettings(String host, int port, ConnectionSecurity connectionSecurity,
                                  AuthType authenticationType, String username, String password,
                                  String clientCertificateAlias, String alias,
                                  String path, String authPath, String mailboxPath) {
        super(Type.WebDAV, host, port, connectionSecurity, authenticationType, username,
                password, clientCertificateAlias);
        this.alias = alias;
        this.path = path;
        this.authPath = authPath;
        this.mailboxPath = mailboxPath;
    }

    @Override
    public Map<String, String> getExtra() {
        Map<String, String> extra = new HashMap<String, String>();
        putIfNotNull(extra, ALIAS_KEY, alias);
        putIfNotNull(extra, PATH_KEY, path);
        putIfNotNull(extra, AUTH_PATH_KEY, authPath);
        putIfNotNull(extra, MAILBOX_PATH_KEY, mailboxPath);
        return extra;
    }

    @Override
    public ServerSettings newPassword(String newPassword) {
        return new WebDavStoreSettings(host, port, connectionSecurity, authenticationType,
                username, newPassword, clientCertificateAlias, alias, path, authPath, mailboxPath);
    }
}
