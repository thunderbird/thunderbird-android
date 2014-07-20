package com.fsck.k9.mail;

import android.content.Context;
import android.widget.ArrayAdapter;
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

    PLAIN(R.string.account_setup_auth_type_normal_password){

        @Override
        public void useInsecureText(boolean insecure, ArrayAdapter<AuthType> authTypesAdapter) {
            if (insecure) {
                mResourceId = R.string.account_setup_auth_type_insecure_password;
            } else {
                mResourceId = R.string.account_setup_auth_type_normal_password;
            }
            authTypesAdapter.notifyDataSetChanged();
        }
    },

    CRAM_MD5(R.string.account_setup_auth_type_encrypted_password),

    EXTERNAL(R.string.account_setup_auth_type_tls_client_certificate),

    /*
     * The following are obsolete authentication settings that were used with
     * SMTP. They are no longer presented to the user as options, but they may
     * still exist in a user's settings from a previous version or may be found
     * when importing settings.
     */
    AUTOMATIC(0),

    LOGIN(0);

    static public ArrayAdapter<AuthType> getArrayAdapter(Context context) {
        AuthType[] authTypes = new AuthType[]{PLAIN, CRAM_MD5, EXTERNAL};
        ArrayAdapter<AuthType> authTypesAdapter = new ArrayAdapter<AuthType>(context,
                android.R.layout.simple_spinner_item, authTypes);
        authTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return authTypesAdapter;
    }

    int mResourceId;

    private AuthType(int id) {
        mResourceId = id;
    }

    /**
     * Used to select an appropriate localized text label for the
     * {@code AuthType.PLAIN} option presented to users.
     *
     * @param insecure
     *            <p>
     *            A value of {@code true} will use "Normal password".
     *            <p>
     *            A value of {@code false} will use
     *            "Password, transmitted insecurely"
     */
    public void useInsecureText(boolean insecure, ArrayAdapter<AuthType> authTypesAdapter) {
        // Do nothing.  Overridden in AuthType.PLAIN
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
