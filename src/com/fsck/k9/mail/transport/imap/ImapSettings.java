package com.fsck.k9.mail.transport.imap;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.store.imap.ImapStore;

/**
 * Settings source for IMAP. Implemented in order to remove coupling between {@link ImapStore} and {@link com.fsck.k9.mail.store.ImapConnection}.
 */
public interface ImapSettings {
    String getHost();

    int getPort();

    ConnectionSecurity getConnectionSecurity();

    AuthType getAuthType();

    String getUsername();

    String getPassword();

    String getClientCertificateAlias();

    boolean useCompression(int type);

    String getPathPrefix();

    void setPathPrefix(String prefix);

    String getPathDelimeter();

    void setPathDelimeter(String delimeter);

    String getCombinedPrefix();

    void setCombinedPrefix(String prefix);
}
