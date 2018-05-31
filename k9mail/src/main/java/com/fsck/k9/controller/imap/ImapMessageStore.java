package com.fsck.k9.controller.imap;


import com.fsck.k9.Account;
import com.fsck.k9.controller.BackendStorage;
import com.fsck.k9.controller.RemoteMessageStore;
import com.fsck.k9.controller.SyncListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.store.imap.ImapStore;


public class ImapMessageStore implements RemoteMessageStore {
    private final ImapSync imapSync;


    // TODO: Pass in SyncConfig interface instead of Account, LocalMessageStore interface instead of LocalStore
    public ImapMessageStore(Account account, BackendStorage backendStorage, ImapStore imapStore) {
        this.imapSync = new ImapSync(account, backendStorage, imapStore);
    }

    @Override
    public void sync(String folder, SyncListener listener, Folder providedRemoteFolder) {
        imapSync.sync(folder, listener, providedRemoteFolder);
    }
}
