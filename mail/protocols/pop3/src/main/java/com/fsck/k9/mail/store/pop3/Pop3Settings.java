package com.fsck.k9.mail.store.pop3;


import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;


interface Pop3Settings {
    String getHost();

    int getPort();

    ConnectionSecurity getConnectionSecurity();

    AuthType getAuthType();

    String getUsername();

    String getPassword();

    String getClientCertificateAlias();
}
