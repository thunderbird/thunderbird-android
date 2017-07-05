package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.selectedstate.response.SelectedStateResponse;


public class UidStoreCommand extends FolderSelectedStateCommand {
    private boolean value;
    private Set<Flag> flagSet;
    private boolean canCreateForwardedFlag;

    private UidStoreCommand() {
    }

    @Override
    String createCommandString() {
        return String.format("%s %s%sFLAGS.SILENT (%s)", Commands.UID_STORE, createCombinedIdString(),
                value ? "+" : "-", ImapUtility.combineFlags(flagSet, canCreateForwardedFlag));
    }

    @Override
    public SelectedStateResponse execute(ImapConnection connection, ImapFolder folder) throws MessagingException {
        try {
            executeInternal(connection, folder);
            //These results are not important, because of the FLAGS.SILENT option
            return null;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(connection, ioe);
        }

    }

    @Override
    Builder newBuilder() {
        return new Builder()
                .value(value)
                .flagSet(flagSet)
                .canCreateForwardedFlag(canCreateForwardedFlag);
    }

    public static class Builder extends FolderSelectedStateCommand.Builder<UidStoreCommand, Builder> {

        public Builder value(boolean value) {
            command.value = value;
            return builder;
        }

        public Builder flagSet(Set<Flag> flagSet) {
            command.flagSet = flagSet;
            return builder;
        }

        public Builder canCreateForwardedFlag(boolean canCreateForwardedFlag) {
            command.canCreateForwardedFlag = canCreateForwardedFlag;
            return builder;
        }

        @Override
        UidStoreCommand createCommand() {
            return new UidStoreCommand();
        }

        @Override
        Builder createBuilder() {
            return this;
        }
    }
}
