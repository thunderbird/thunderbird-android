package com.fsck.k9.mail.store.imap.command;


import java.io.IOException;
import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.response.CopyUidResponse;


public class UidCopyCommand extends SelectByIdCommand {

    private String destinationFolderName;

    private UidCopyCommand(ImapCommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    public String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_COPY).append(" ");
        super.addIds(builder);
        addDestinationFolderName(builder);
        return builder.toString().trim();
    }

    @Override
    public CopyUidResponse execute() throws MessagingException {

        try {
            List<List<ImapResponse>> responses = executeInternal(false);
            CopyUidResponse response = CopyUidResponse.parse(commandFactory, responses);
            folder.handleUntaggedResponses(response);
            return response;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(commandFactory.getConnection(), ioe);
        }

    }

    private void addDestinationFolderName(StringBuilder builder) {
        builder.append(destinationFolderName);
    }

    @Override
    Builder newBuilder() {
        return new Builder(commandFactory, folder)
                .useUids(useUids)
                .idSet(idSet)
                .idRanges(idRanges)
                .destinationFolderName(destinationFolderName);
    }

    public static class Builder extends SelectByIdCommand.Builder<UidCopyCommand, Builder> {

        public Builder(ImapCommandFactory commandFactory, ImapFolder folder) {
            super(commandFactory, folder);
        }

        public Builder destinationFolderName(String destinationFolderName) {
            command.destinationFolderName = destinationFolderName;
            return builder;
        }

        @Override
        UidCopyCommand createCommand() {
            return new UidCopyCommand(null);
        }

        @Override
        Builder createBuilder() {
            return this;
        }

    }
}


