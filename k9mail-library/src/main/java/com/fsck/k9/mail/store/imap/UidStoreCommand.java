package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.Flag;


class UidStoreCommand extends FolderSelectedStateCommand<SelectedStateResponse> {
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
    public SelectedStateResponse parseResponses(List<ImapResponse> unparsedResponses) {
        //These results are not important, because of the FLAGS.SILENT option
        return null;
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
