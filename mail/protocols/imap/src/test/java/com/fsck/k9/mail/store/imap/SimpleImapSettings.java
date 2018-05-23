package com.fsck.k9.mail.store.imap;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.NetworkType;


class SimpleImapSettings implements ImapSettings {
    private String host;
    private int port;
    private ConnectionSecurity connectionSecurity = ConnectionSecurity.NONE;
    private AuthType authType;
    private String username;
    private String password;
    private String pathPrefix;
    private String pathDelimiter;
    private String combinedPrefix;
    private boolean useCompression = false;


    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public ConnectionSecurity getConnectionSecurity() {
        return connectionSecurity;
    }

    @Override
    public AuthType getAuthType() {
        return authType;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getClientCertificateAlias() {
        return null;
    }

    @Override
    public boolean useCompression(NetworkType type) {
        return useCompression;
    }

    @Override
    public String getPathPrefix() {
        return pathPrefix;
    }

    @Override
    public void setPathPrefix(String prefix) {
        pathPrefix = prefix;
    }

    @Override
    public String getPathDelimiter() {
        return pathDelimiter;
    }

    @Override
    public void setPathDelimiter(String delimiter) {
        pathDelimiter = delimiter;
    }

    @Override
    public String getCombinedPrefix() {
        return combinedPrefix;
    }

    @Override
    public void setCombinedPrefix(String prefix) {
        combinedPrefix = prefix;
    }

    void setHost(String host) {
        this.host = host;
    }

    void setPort(int port) {
        this.port = port;
    }

    void setConnectionSecurity(ConnectionSecurity connectionSecurity) {
        this.connectionSecurity = connectionSecurity;
    }

    void setAuthType(AuthType authType) {
        this.authType = authType;
    }

    void setUsername(String username) {
        this.username = username;
    }

    void setPassword(String password) {
        this.password = password;
    }

    void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }
}
