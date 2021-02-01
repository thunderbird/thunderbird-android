package com.fsck.k9.mail.store.webdav;


import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.ServerSettings;


/**
 * Extract WebDav-specific server settings from {@link ServerSettings}.
 */
public class WebDavStoreSettings {
    private static final String ALIAS_KEY = "alias";
    private static final String PATH_KEY = "path";
    private static final String AUTH_PATH_KEY = "authPath";
    private static final String MAILBOX_PATH_KEY = "mailboxPath";

    public static String getAlias(ServerSettings serverSettings) {
        return serverSettings.getExtra().get(ALIAS_KEY);
    }

    public static String getPath(ServerSettings serverSettings) {
        return serverSettings.getExtra().get(PATH_KEY);
    }

    public static String getAuthPath(ServerSettings serverSettings) {
        return serverSettings.getExtra().get(AUTH_PATH_KEY);
    }

    public static String getMailboxPath(ServerSettings serverSettings) {
        return serverSettings.getExtra().get(MAILBOX_PATH_KEY);
    }

    public static Map<String, String> createExtra(String alias, String path, String authPath, String mailboxPath) {
        Map<String, String> extra = new HashMap<>();
        putIfNotNull(extra, ALIAS_KEY, alias);
        putIfNotNull(extra, PATH_KEY, path);
        putIfNotNull(extra, AUTH_PATH_KEY, authPath);
        putIfNotNull(extra, MAILBOX_PATH_KEY, mailboxPath);
        return extra;
    }

    private static void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
