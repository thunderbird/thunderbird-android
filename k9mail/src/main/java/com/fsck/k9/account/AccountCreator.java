package com.fsck.k9.account;

import com.fsck.k9.Account.DeletePolicy;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Deals with logic surrounding account creation.
 * <p/>
 * TODO Move much of the code from com.fsck.k9.activity.setup.* into here
 */
public class AccountCreator {

    private static Map<Type, DeletePolicy> defaults = new HashMap<Type, DeletePolicy>();

    static {
        defaults.put(Type.IMAP, DeletePolicy.ON_DELETE);
        defaults.put(Type.POP3, DeletePolicy.NEVER);
        defaults.put(Type.WebDAV, DeletePolicy.ON_DELETE);
    }

    public static DeletePolicy calculateDefaultDeletePolicy(Type type) {
        return defaults.get(type);
    }

    public static int getDefaultPort(ConnectionSecurity securityType, Type storeType) {
        switch (securityType) {
        case NONE:
        case STARTTLS_REQUIRED:
            return storeType.defaultPort;
        case SSL_TLS_REQUIRED:
            return storeType.defaultTlsPort;
        default:
            throw new AssertionError("Unhandled ConnectionSecurity type encountered: " + securityType);
        }
    }

}
