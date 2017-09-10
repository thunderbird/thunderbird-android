package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.Collections;
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
    private Set<Flag> flags;
    private boolean canCreateForwardedFlag;

    private UidStoreCommand(Set<Long> uids, boolean value, Set<Flag> flags,
                            boolean canCreateForwardedFlag) {
        super(uids);
        this.value = value;
        this.flags = flags;
        this.canCreateForwardedFlag = canCreateForwardedFlag;
    }

    @Override
    String createCommandString() {
        return String.format("%s %s%sFLAGS.SILENT (%s)", Commands.UID_STORE, createCombinedIdString(),
                value ? "+" : "-", ImapUtility.combineFlags(flags, canCreateForwardedFlag));
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

    public static UidStoreCommand createWithUids(Set<Long> uids, boolean value, Set<Flag> flags,
                                         boolean canCreateForwardedFlag) {
        return new UidStoreCommand(uids, value, flags, canCreateForwardedFlag);
    }

    public static UidStoreCommand createWithAllUids(boolean value, Set<Flag> flags,
                                            boolean canCreateForwardedFlag) {
        UidStoreCommand command = new UidStoreCommand(Collections.<Long>emptySet(), value, flags,
                canCreateForwardedFlag);
        command.useAllIds(true);
        return command;
    }
}
