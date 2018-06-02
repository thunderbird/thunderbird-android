package com.fsck.k9.backend.imap;


import com.fsck.k9.backend.api.Backend;
import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.store.imap.ImapStore;


public class ImapBackend implements Backend {
    private final ImapSync imapSync;


    public ImapBackend(String accountName, BackendStorage backendStorage, ImapStore imapStore) {
        this.imapSync = new ImapSync(accountName, backendStorage, imapStore);
    }

    @Override
    public void sync(String folder, SyncConfig syncConfig, SyncListener listener, Folder providedRemoteFolder) {
        imapSync.sync(folder, syncConfig, listener, providedRemoteFolder);
    }
}
