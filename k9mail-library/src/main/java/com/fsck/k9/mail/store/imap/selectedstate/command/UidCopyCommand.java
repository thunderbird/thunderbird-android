package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.selectedstate.response.UidCopyResponse;


public class UidCopyCommand extends SelectedStateCommand {

    private String destinationFolderName;

    private UidCopyCommand(ImapConnection connection, ImapFolder folder) {
        super(connection, folder);
    }

    @Override
    public String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_COPY).append(" ");
        super.addIds(builder);
        addDestinationFolderName(builder);
        return builder.toString().trim();
    }

    @Override
    public UidCopyResponse execute() throws MessagingException {

        try {
            List<List<ImapResponse>> responses = executeInternal();
            return UidCopyResponse.parse(responses);
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(connection, ioe);
        }

    }

    private void addDestinationFolderName(StringBuilder builder) {
        builder.append(destinationFolderName);
    }

    @Override
    Builder newBuilder() {
        return new Builder(connection, folder)
                .destinationFolderName(destinationFolderName);
    }

    public static class Builder extends SelectedStateCommand.Builder<UidCopyCommand, Builder> {

        public Builder(ImapConnection connection, ImapFolder folder) {
            super(connection, folder);
        }

        public Builder destinationFolderName(String destinationFolderName) {
            command.destinationFolderName = destinationFolderName;
            return builder;
        }

        @Override
        UidCopyCommand createCommand(ImapConnection connection, ImapFolder folder) {
            return new UidCopyCommand(connection, folder);
        }

        @Override
        Builder createBuilder() {
            return this;
        }

    }
}


