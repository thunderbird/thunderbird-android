package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.selectedstate.response.UidCopyResponse;


public class UidCopyCommand extends FolderSelectedStateCommand {
    private String destinationFolderName;

    private UidCopyCommand(Set<Long> uids, String destinationFolderName) {
        super(uids);
        this.destinationFolderName = destinationFolderName;
    }

    @Override
    String createCommandString() {
        return String.format("%s %s%s", Commands.UID_COPY, createCombinedIdString(), destinationFolderName);
    }

    @Override
    public UidCopyResponse execute(ImapConnection connection, ImapFolder folder) throws MessagingException {
        try {
            List<List<ImapResponse>> responses = executeInternal(connection, folder);
            return UidCopyResponse.parse(responses);
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(connection, ioe);
        }
    }

    public static UidCopyCommand createWithUids(Set<Long> uids, String destinationFolderName) {
        return new UidCopyCommand(uids, destinationFolderName);
    }
}


