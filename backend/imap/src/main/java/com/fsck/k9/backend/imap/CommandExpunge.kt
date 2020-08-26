package com.fsck.k9.backend.imap;


import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
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

        ImapFolder remoteFolder = imapStore.getFolder(folderServerId);
        try {
            if (!remoteFolder.exists()) {
                return;
            }
            remoteFolder.open(ImapFolder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != ImapFolder.OPEN_MODE_RW) {
                return;
            }
            remoteFolder.expunge();

            Timber.d("processPendingExpunge: complete for folder = %s", folderServerId);
        } finally {
            remoteFolder.close();
        }
    }

    void expungeMessages(@NotNull String folderServerId, @NotNull List<String> messageServerIds)
            throws MessagingException {
        ImapFolder remoteFolder = imapStore.getFolder(folderServerId);
        try {
            if (!remoteFolder.exists()) {
                return;
            }
            remoteFolder.open(ImapFolder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != ImapFolder.OPEN_MODE_RW) {
                return;
            }
            remoteFolder.expungeUids(messageServerIds);
        } finally {
            remoteFolder.close();
        }
    }
}
