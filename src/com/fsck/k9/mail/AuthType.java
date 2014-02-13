package com.fsck.k9.mail;

import com.fsck.k9.K9;
import com.fsck.k9.R;

public enum AuthType {

    /*
     * The names of these auth. types are saved as strings when settings are
     * exported, and are also saved as part of the Server URI saved in the
     * account settings.
     */
    AUTOMATIC(R.string.account_setup_auth_type_automatic),
    PLAIN(R.string.account_setup_auth_type_normal_password),
    CRAM_MD5(R.string.account_setup_auth_type_encrypted_password),
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
