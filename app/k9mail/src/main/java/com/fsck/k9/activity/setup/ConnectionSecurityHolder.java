package com.fsck.k9.activity.setup;

import android.content.res.Resources;

import com.fsck.k9.R;
import com.fsck.k9.mail.ConnectionSecurity;

class ConnectionSecurityHolder {
    final ConnectionSecurity connectionSecurity;
    private final Resources resources;

    public ConnectionSecurityHolder(ConnectionSecurity connectionSecurity, Resources resources) {
        this.connectionSecurity = connectionSecurity;
        this.resources = resources;
    }

    public String toString() {
        final int resourceId = resourceId();
        if (resourceId == 0) {
            return connectionSecurity.name();
        } else {
            return resources.getString(resourceId);
        }
    }

    private int resourceId() {
        switch (connectionSecurity) {
            case NONE: return R.string.account_setup_incoming_security_none_label;
            case STARTTLS_REQUIRED: return R.string.account_setup_incoming_security_tls_label;
            case SSL_TLS_REQUIRED:  return R.string.account_setup_incoming_security_ssl_label;
            default: return 0;
        }
    }
}
