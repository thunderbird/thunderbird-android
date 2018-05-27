package com.fsck.k9.controller.imap;


import com.fsck.k9.Account;
import com.fsck.k9.controller.RemoteMessageStore;
import com.fsck.k9.controller.SyncListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.notification.NotificationController;


public class ImapMessageStore implements RemoteMessageStore {
    private final ImapSync imapSync;


    public ImapMessageStore(NotificationController notificationController) {
        this.imapSync = new ImapSync(notificationController);
    }

    @Override
    public void sync(Account account, String folder, SyncListener listener, Folder providedRemoteFolder) {
        imapSync.sync(account, folder, listener, providedRemoteFolder);
    }
}
