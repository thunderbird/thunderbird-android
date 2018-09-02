package com.fsck.k9.mail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Generic container to hold server settings.
 */
public class ServerSettings {
    /**
     * Name of the protocol these server settings belong to. Must be all lower case.
     */
    public final String type;

    /**
     * The host name of the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String host;

    /**
     * The port number of the server.
     *
     * {@code -1} if not applicable for the store or transport.
     */
    public final int port;

    /**
     * The type of connection security to be used when connecting to the server.
     *
     * {@link ConnectionSecurity#NONE} if not applicable for the store or transport.
     */
    public final ConnectionSecurity connectionSecurity;

    /**
     * The authentication method to use when connecting to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final AuthType authenticationType;

    /**
     * The username part of the credentials needed to authenticate to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String username;

    /**
     * The password part of the credentials needed to authenticate to the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String password;

    /**
     * The alias to retrieve a client certificate using Android 4.0 KeyChain API
     * for TLS client certificate authentication with the server.
     *
     * {@code null} if not applicable for the store or transport.
     */
    public final String clientCertificateAlias;

    /**
     * Store- or transport-specific settings as key/value pair.
     *
     * {@code null} if not applicable for the store or transport.
     */
    private final Map<String, String> extra;


    /**
     * Creates a new {@code ServerSettings} object.
     *
     * @param type
     *         see {@link ServerSettings#type}
     * @param host
     *         see {@link ServerSettings#host}
     * @param port
     *         see {@link ServerSettings#port}
     * @param connectionSecurity
     *         see {@link ServerSettings#connectionSecurity}
     * @param authenticationType
     *         see {@link ServerSettings#authenticationType}
     * @param username
     *         see {@link ServerSettings#username}
     * @param password
     *         see {@link ServerSettings#password}
     * @param clientCertificateAlias
     *         see {@link ServerSettings#clientCertificateAlias}
     */
    public ServerSettings(String type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias) {
        this.type = checkType(type);
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.username = username;
        this.password = password;
        this.clientCertificateAlias = clientCertificateAlias;
        this.extra = null;
    }

    /**
     * Creates a new {@code ServerSettings} object.
     *
     * @param type
     *         see {@link ServerSettings#type}
     * @param host
     *         see {@link ServerSettings#host}
     * @param port
     *         see {@link ServerSettings#port}
     * @param connectionSecurity
     *         see {@link ServerSettings#connectionSecurity}
     * @param authenticationType
     *         see {@link ServerSettings#authenticationType}
     * @param username
     *         see {@link ServerSettings#username}
     * @param password
     *         see {@link ServerSettings#password}
     * @param clientCertificateAlias
     *         see {@link ServerSettings#clientCertificateAlias}
     * @param extra
     *         see {@link ServerSettings#extra}
     */
    public ServerSettings(String type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias, Map<String, String> extra) {
        this.type = checkType(type);
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.username = username;
        this.password = password;
        this.clientCertificateAlias = clientCertificateAlias;
        this.extra = (extra != null) ? Collections.unmodifiableMap(new HashMap<>(extra)) : null;
    }

    /**
     * Creates an "empty" {@code ServerSettings} object.
     *
     * Everything but {@link ServerSettings#type} is unused.
     *
     * @param type
     *         see {@link ServerSettings#type}
     */
    public ServerSettings(String type) {
        this.type = checkType(type);
        host = null;
        port = -1;
        connectionSecurity = ConnectionSecurity.NONE;
        authenticationType = null;
        username = null;
        password = null;
        clientCertificateAlias = null;
        extra = null;
    }

    /**
     * Returns store- or transport-specific settings as key/value pair.
     *
     * @return additional set of settings as key/value pair.
     */
    public Map<String, String> getExtra() {
        return extra;
    }

    protected void putIfNotNull(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public ServerSettings newPassword(String newPassword) {
        return new ServerSettings(type, host, port, connectionSecurity, authenticationType,
                username, newPassword, clientCertificateAlias);
    }

    public ServerSettings newClientCertificateAlias(String newAlias) {
        return new ServerSettings(type, host, port, connectionSecurity, AuthType.EXTERNAL,
                username, password, newAlias);
    }

    private String checkType(String type) {
        if (type == null) {
            throw new IllegalArgumentException("type == null");
        }

        if (!type.equals(type.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("type must be all lower case");
        }

        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof ServerSettings)) {
            return false;
        }
        ServerSettings that = (ServerSettings) obj;
        return type.equals(that.type) &&
                port == that.port &&
                connectionSecurity == that.connectionSecurity &&
                authenticationType == that.authenticationType &&
                (host == null ? that.host == null : host.equals(that.host)) &&
                (username == null ? that.username == null : username.equals(that.username)) &&
                (password == null ? that.password == null : password.equals(that.password)) &&
                (clientCertificateAlias == null ? that.clientCertificateAlias == null :
                        clientCertificateAlias.equals(that.clientCertificateAlias));
    }
}
