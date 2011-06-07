package com.fsck.k9.mail;

import java.util.Map;
import com.fsck.k9.Account;

/**
 * This is an abstraction to get rid of the store-specific URIs.
 *
 * <p>
 * Right now it's only used for settings import/export. But the goal is to get rid of
 * store URIs altogether.
 * </p>
 *
 * @see Account#getStoreUri()
 */
public class StoreSettings {
    /**
     * Name of the store type (e.g. "IMAP").
     */
    public final String type;

    /**
     * The host name of the incoming server.
     */
    public final String host;

    /**
     * The port number of the incoming server.
     */
    public final int port;

    /**
     * The type of connection security to be used when connecting to the incoming server.
     *
     * {@link ConnectionSecurity#NONE} if not applicable for the store.
     */
    public final ConnectionSecurity connectionSecurity;

    /**
     * The authentication method to use when connecting to the incoming server.
     *
     * {@code null} if not applicable for the store.
     */
    public final String authenticationType;

    /**
     * The username part of the credentials needed to authenticate to the incoming server.
     *
     * {@code null} if unused or not applicable for the store.
     */
    public final String username;

    /**
     * The password part of the credentials needed to authenticate to the incoming server.
     *
     * {@code null} if unused or not applicable for the store.
     */
    public final String password;


    /**
     * Creates a new {@code StoreSettings} object.
     *
     * @param type
     *         see {@link StoreSettings#type}
     * @param host
     *         see {@link StoreSettings#host}
     * @param port
     *         see {@link StoreSettings#port}
     * @param connectionSecurity
     *         see {@link StoreSettings#connectionSecurity}
     * @param authenticationType
     *         see {@link StoreSettings#authenticationType}
     * @param username
     *         see {@link StoreSettings#username}
     * @param password
     *         see {@link StoreSettings#password}
     */
    public StoreSettings(String type, String host, int port,
            ConnectionSecurity connectionSecurity, String authenticationType, String username,
            String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns store-specific settings as key/value pair.
     *
     * <p>Classes that inherit from this one are expected to override this method.</p>
     */
    public Map<String, String> getExtra() {
        return null;
    }

    protected void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}