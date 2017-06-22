package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.selectedstate.response.SelectedStateResponse;


public class UidStoreCommand extends SelectedStateCommand {

    private boolean value;
    private Set<Flag> flagSet;

    private UidStoreCommand(ImapConnection connection, ImapFolder folder) {
        super(connection, folder);
    }

    @Override
    public String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_STORE).append(" ");
        super.addIds(builder);
        addValue(builder);
        addFlagSet(builder);
        return builder.toString().trim();
    }

    @Override
    public SelectedStateResponse execute() throws MessagingException {

        try {
            executeInternal();
            //These results are not important, because of the FLAGS.SILENT option
            return null;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(connection, ioe);
        }

    }

    private void addValue(StringBuilder builder) {
        builder.append(value  ? "+" : "-");
    }

    private void addFlagSet(StringBuilder builder) {
        builder.append("FLAGS.SILENT (").append(folder.combineFlags(flagSet)).append(")");
    }

    @Override
    Builder newBuilder() {
        return new Builder(connection, folder)
                .value(value)
                .flagSet(flagSet);
    }

    public static class Builder extends SelectedStateCommand.Builder<UidStoreCommand, Builder> {

        public Builder(ImapConnection connection, ImapFolder folder) {
            super(connection, folder);
        }

        public Builder value(boolean value) {
            command.value = value;
            return builder;
        }

        public Builder flagSet(Set<Flag> flagSet) {
            command.flagSet = flagSet;
            return builder;
        }

        @Override
        UidStoreCommand createCommand(ImapConnection connection, ImapFolder folder) {
            return new UidStoreCommand(connection, folder);
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}
