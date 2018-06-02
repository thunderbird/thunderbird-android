package com.fsck.k9.controller.imap;


import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.RemoteMessageStore;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.store.imap.ImapStore;


public class ImapMessageStore implements RemoteMessageStore {
    private final ImapSync imapSync;


    // TODO: Pass in SyncConfig interface instead of Account, LocalMessageStore interface instead of LocalStore
    public ImapMessageStore(String accountName, BackendStorage backendStorage, ImapStore imapStore) {
        this.imapSync = new ImapSync(accountName, backendStorage, imapStore);
    }

    @Override
    public void sync(String folder, SyncConfig syncConfig, SyncListener listener, Folder providedRemoteFolder) {
        imapSync.sync(folder, syncConfig, listener, providedRemoteFolder);
    }
}
