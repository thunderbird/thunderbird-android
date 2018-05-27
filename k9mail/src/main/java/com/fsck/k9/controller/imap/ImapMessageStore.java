package com.fsck.k9.controller.imap;


import com.fsck.k9.Account;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.RemoteMessageStore;
import com.fsck.k9.controller.SyncListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.notification.NotificationController;


public class ImapMessageStore implements RemoteMessageStore {
    private final ImapSync imapSync;


    public ImapMessageStore(NotificationController notificationController, MessagingController controller) {
        this.imapSync = new ImapSync(notificationController, controller);
    }

    @Override
    public void sync(Account account, String folder, SyncListener listener, Folder providedRemoteFolder) {
        imapSync.sync(account, folder, listener, providedRemoteFolder);
    }
}
