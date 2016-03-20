package com.fsck.k9.crypto;

import android.text.TextUtils;

import com.fsck.k9.Identity;


public class SmimeApiHelper {

    /**
     * Create an "account name" from the supplied identity for use with the Smime API's
     * <code>EXTRA_ACCOUNT_NAME</code>.
     *
     * @return A string with the following format:
     *         <code>display name &lt;user@example.com&gt;</code>
     *
     * @see org.openintents.smime.util.SmimeApi#EXTRA_ACCOUNT_NAME
     */
    public static String buildUserId(Identity identity) {
        StringBuilder sb = new StringBuilder();

        String name = identity.getName();
        if (!TextUtils.isEmpty(name)) {
            sb.append(name).append(" ");
        }
        sb.append("<").append(identity.getEmail()).append(">");

        return sb.toString();
    }
}
