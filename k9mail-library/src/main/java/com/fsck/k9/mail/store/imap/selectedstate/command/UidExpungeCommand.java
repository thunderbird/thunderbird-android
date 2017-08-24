package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.selectedstate.response.SelectedStateResponse;


public class UidExpungeCommand extends FolderSelectedStateCommand {

    private UidExpungeCommand() {
    }

    @Override
    String createCommandString() {
        return String.format("%s %s", Commands.UID_EXPUNGE, createCombinedIdString()).trim();
    }

    @Override
    public SelectedStateResponse execute(ImapConnection connection, ImapFolder folder) throws MessagingException {
        try {
            executeInternal(connection, folder);
            return null;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(connection, ioe);
        }

    }

    @Override
    Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends FolderSelectedStateCommand.Builder<UidExpungeCommand, Builder> {

        @Override
        UidExpungeCommand createCommand() {
            return new UidExpungeCommand();
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}
