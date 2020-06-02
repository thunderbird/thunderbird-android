package com.fsck.k9.backend.imap;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapStore;
import org.jetbrains.annotations.NotNull;


class CommandSetFlag {
    private final ImapStore imapStore;


    CommandSetFlag(ImapStore imapStore) {
        this.imapStore = imapStore;
    }

    void setFlag(@NotNull String folderServerId, @NotNull List<String> messageServerIds, @NotNull Flag flag,
            boolean newState) throws MessagingException {

        ImapFolder remoteFolder = imapStore.getFolder(folderServerId);
        if (!remoteFolder.exists()) {
            return;
        }

        try {
            remoteFolder.open(ImapFolder.OPEN_MODE_RW);
            if (remoteFolder.getMode() != ImapFolder.OPEN_MODE_RW) {
                return;
            }
            List<ImapMessage> messages = new ArrayList<>();
            for (String uid : messageServerIds) {
                messages.add(remoteFolder.getMessage(uid));
            }

            if (messages.isEmpty()) {
                return;
            }
            remoteFolder.setFlags(messages, Collections.singleton(flag), newState);
        } finally {
            remoteFolder.close();
        }
    }
}
