package com.fsck.k9.mail;

import com.fsck.k9.K9;
import com.fsck.k9.R;

public enum AuthType {

    /*
     * The names of these authentication types are saved as strings when
     * settings are exported and are also saved as part of the Server URI stored
     * in the account settings.
     *
     * PLAIN and CRAM_MD5 originally referred to specific SASL authentication
     * mechanisms. Their meaning has since been broadened to mean authentication
     * with unencrypted and encrypted passwords, respectively. Nonetheless,
     * their original names have been retained for backward compatibility with
     * user settings.
     */
    PLAIN(R.string.account_setup_auth_type_normal_password),
    CRAM_MD5(R.string.account_setup_auth_type_encrypted_password),

    /*
     * The following are obsolete authentication settings that were used with
     * SMTP. They are no longer presented to the user as options, but they may
     * still exist in a user's settings from a previous version or may be found
     * when importing settings.
     */
    AUTOMATIC(0),

    LOGIN(0);

    private final int mResourceId;

    private AuthType(int id) {
        mResourceId = id;
    }

    @Override
    public String toString() {
        if (mResourceId == 0) {
            return name();
        } else {
            return K9.app.getString(mResourceId);
        }
    }
}
