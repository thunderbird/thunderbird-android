package com.fsck.k9.mail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is an abstraction to get rid of the store- and transport-specific URIs.
 *
 * <p>
 * Right now it's only used for settings import/export. But the goal is to get rid of
 * store/transport URIs altogether.
 * </p>
 *
 * @see com.fsck.k9.mail.store.StoreConfig#getStoreUri()
 * @see com.fsck.k9.mail.store.StoreConfig#getTransportUri()
 */
public class ServerSettings {

    public enum Type {

        IMAP(143, 993),
        SMTP(587, 465),
        WebDAV(80, 443),
        POP3(110, 995);

        public final int defaultPort;

        /**
         * Note: port for connections using TLS (=SSL) immediately
         * from the initial TCP connection.
         *
         * STARTTLS uses the defaultPort, then upgrades.
         *
         * See https://www.fastmail.com/help/technical/ssltlsstarttls.html.
         */
        public final int defaultTlsPort;

        private Type(int defaultPort, int defaultTlsPort) {
            this.defaultPort = defaultPort;
            this.defaultTlsPort = defaultTlsPort;
        }
    }

    /**
     * Name of the store or transport type (e.g. IMAP).
     */
    public final Type type;

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
    public ServerSettings(Type type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias) {
        this.type = type;
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
    public ServerSettings(Type type, String host, int port,
            ConnectionSecurity connectionSecurity, AuthType authenticationType, String username,
            String password, String clientCertificateAlias, Map<String, String> extra) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.connectionSecurity = connectionSecurity;
        this.authenticationType = authenticationType;
        this.username = username;
        this.password = password;
        this.clientCertificateAlias = clientCertificateAlias;
        this.extra = (extra != null) ?
                Collections.unmodifiableMap(new HashMap<String, String>(extra)) : null;
    }

    /**
     * Creates an "empty" {@code ServerSettings} object.
     *
     * Everything but {@link ServerSettings#type} is unused.
     *
     * @param type
     *         see {@link ServerSettings#type}
     */
    public ServerSettings(Type type) {
        this.type = type;
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
}