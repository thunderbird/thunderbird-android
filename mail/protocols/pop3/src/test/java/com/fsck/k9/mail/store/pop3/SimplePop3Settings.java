package com.fsck.k9.mail.store.pop3;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;

class SimplePop3Settings implements Pop3Settings {
    private String host;
    private int port;
    private ConnectionSecurity connectionSecurity = ConnectionSecurity.NONE;
    private AuthType authType;
    private String username;
    private String password;

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
}
