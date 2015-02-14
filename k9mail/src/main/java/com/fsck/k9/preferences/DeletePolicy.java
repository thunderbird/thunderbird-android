package com.fsck.k9.preferences;

import android.util.Log;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.store.pop3.Pop3Store;
import com.fsck.k9.mail.store.webdav.WebDavStore;

/**
 * Defaults for mail deletion.
 */
public class DeletePolicy {

    public static int calculateDefaultDeletePolicy(ServerSettings settings) {

        if (ImapStore.STORE_TYPE.equals(settings.type)) {
            return Account.DELETE_POLICY_ON_DELETE;

        } else if (Pop3Store.STORE_TYPE.equals(settings.type)) {
            return Account.DELETE_POLICY_NEVER;

        } else if (WebDavStore.STORE_TYPE.equals(settings.type)) {
            return Account.DELETE_POLICY_ON_DELETE;

        } else {
            Log.w(K9.LOG_TAG, "Unknown account type: " + settings.type);
            // a safe default, but shouldn't happen
            return Account.DELETE_POLICY_NEVER;
        }
    }
}
