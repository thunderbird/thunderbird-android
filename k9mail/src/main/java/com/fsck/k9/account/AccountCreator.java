package com.fsck.k9.account;


import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings.Type;


/**
 * Deals with logic surrounding account creation.
 * <p/>
 * TODO Move much of the code from com.fsck.k9.activity.setup.* into here
 */
public class AccountCreator {

    public static DeletePolicy getDefaultDeletePolicy(Type type) {
        switch (type) {
            case IMAP: {
                return DeletePolicy.ON_DELETE;
            }
            case POP3: {
                return DeletePolicy.NEVER;
            }
            case WebDAV: {
                return DeletePolicy.ON_DELETE;
            }
            case SMTP: {
                throw new IllegalStateException("Delete policy doesn't apply to SMTP");
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    public static int getDefaultPort(ConnectionSecurity securityType, Type storeType) {
        switch (securityType) {
            case NONE:
            case STARTTLS_REQUIRED: {
                return storeType.defaultPort;
            }
            case SSL_TLS_REQUIRED: {
                return storeType.defaultTlsPort;
            }
        }

        throw new AssertionError("Unhandled ConnectionSecurity type encountered: " + securityType);
    }
}
