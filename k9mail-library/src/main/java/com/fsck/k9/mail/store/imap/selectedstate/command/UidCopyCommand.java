package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.selectedstate.response.UidCopyResponse;


public class UidCopyCommand extends FolderSelectedStateCommand {
    private String destinationFolderName;

    private UidCopyCommand() {
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

    @Override
    Builder newBuilder() {
        return new Builder().destinationFolderName(destinationFolderName);
    }

    public static class Builder extends FolderSelectedStateCommand.Builder<UidCopyCommand, Builder> {

        public Builder destinationFolderName(String destinationFolderName) {
            command.destinationFolderName = destinationFolderName;
            return builder;
        }

        @Override
        UidCopyCommand createCommand() {
            return new UidCopyCommand();
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}


