package com.fsck.k9.backend.webdav;


import java.util.Collections;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.webdav.WebDavFolder;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import org.jetbrains.annotations.NotNull;


class CommandMarkAllAsRead {
    private final WebDavStore webDavStore;


    CommandMarkAllAsRead(WebDavStore webDavStore) {
        this.webDavStore = webDavStore;
    }

    void markAllAsRead(@NotNull String folderServerId) throws MessagingException {
        WebDavFolder remoteFolder = webDavStore.getFolder(folderServerId);
        if (!remoteFolder.exists()) {
            return;
        }

        try {
            remoteFolder.open(Folder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != Folder.OPEN_MODE_RW) {
                return;
            }

            remoteFolder.setFlags(Collections.singleton(Flag.SEEN), true);
        } finally {
            remoteFolder.close();
        }
    }
}
