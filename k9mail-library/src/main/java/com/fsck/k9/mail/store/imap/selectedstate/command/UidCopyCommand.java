package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.selectedstate.response.UidCopyResponse;


public class UidCopyCommand extends FolderSelectedStateCommand<UidCopyResponse> {
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
    public UidCopyResponse parseResponses(List<List<ImapResponse>> unparsedResponses) {
        return UidCopyResponse.parse(unparsedResponses);
    }

    public static UidCopyCommand createWithUids(Set<Long> uids, String destinationFolderName) {
        return new UidCopyCommand(uids, destinationFolderName);
    }
}


