package com.fsck.k9.backend.imap;


import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapStore;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;


class CommandExpunge {
    private final ImapStore imapStore;


    CommandExpunge(ImapStore imapStore) {
        this.imapStore = imapStore;
    }

    void expunge(@NotNull String folderServerId) throws MessagingException {
        Timber.d("processPendingExpunge: folder = %s", folderServerId);

        Folder remoteFolder = imapStore.getFolder(folderServerId);
        try {
            if (!remoteFolder.exists()) {
                return;
            }
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }
            remoteFolder.expunge();

            Timber.d("processPendingExpunge: complete for folder = %s", folderServerId);
        } finally {
            remoteFolder.close();
        }
    }
}
