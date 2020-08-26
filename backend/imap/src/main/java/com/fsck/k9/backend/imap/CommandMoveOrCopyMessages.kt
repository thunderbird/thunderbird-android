package com.fsck.k9.backend.imap;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapStore;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;


class CommandMoveOrCopyMessages {
    private final ImapStore imapStore;


    CommandMoveOrCopyMessages(ImapStore imapStore) {
        this.imapStore = imapStore;
    }

    Map<String, String> moveMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return moveOrCopyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds, false);
    }

    Map<String, String> copyMessages(@NotNull String sourceFolderServerId, @NotNull String targetFolderServerId,
            @NotNull List<String> messageServerIds) throws MessagingException {
        return moveOrCopyMessages(sourceFolderServerId, targetFolderServerId, messageServerIds, true);
    }

    private Map<String, String> moveOrCopyMessages(String srcFolder, String destFolder, Collection<String> uids,
            boolean isCopy) throws MessagingException {
        ImapFolder remoteSrcFolder = null;
        ImapFolder remoteDestFolder = null;

        try {
            remoteSrcFolder = imapStore.getFolder(srcFolder);

            List<ImapMessage> messages = new ArrayList<>();

            for (String uid : uids) {
                messages.add(remoteSrcFolder.getMessage(uid));
            }

            if (messages.isEmpty()) {
                Timber.i("processingPendingMoveOrCopy: no remote messages to move, skipping");
                return null;
            }

            if (!remoteSrcFolder.exists()) {
                throw new MessagingException("processingPendingMoveOrCopy: remoteFolder " + srcFolder +
                        " does not exist", true);
            }

            remoteSrcFolder.open(ImapFolder.OPEN_MODE_RW);
            if (remoteSrcFolder.getMode() != ImapFolder.OPEN_MODE_RW) {
                throw new MessagingException("processingPendingMoveOrCopy: could not open remoteSrcFolder " +
                        srcFolder + " read/write", true);
            }

            Timber.d("processingPendingMoveOrCopy: source folder = %s, %d messages, " +
                    "destination folder = %s, isCopy = %s", srcFolder, messages.size(), destFolder, isCopy);


            remoteDestFolder = imapStore.getFolder(destFolder);

            if (isCopy) {
                return remoteSrcFolder.copyMessages(messages, remoteDestFolder);
            } else {
                return remoteSrcFolder.moveMessages(messages, remoteDestFolder);
            }
        } finally {
            closeFolder(remoteSrcFolder);
            closeFolder(remoteDestFolder);
        }
    }

    private static void closeFolder(ImapFolder folder) {
        if (folder != null) {
            folder.close();
        }
    }
}
