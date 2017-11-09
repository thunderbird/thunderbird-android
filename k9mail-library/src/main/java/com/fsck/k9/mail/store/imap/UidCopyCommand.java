package com.fsck.k9.mail.store.imap;


import java.util.List;
import java.util.Set;


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
    public UidCopyResponse parseResponses(List<ImapResponse> unparsedResponses) {
        return UidCopyResponse.parse(unparsedResponses);
    }

    public static UidCopyCommand createWithUids(Set<Long> uids, String destinationFolderName) {
        return new UidCopyCommand(uids, destinationFolderName);
    }
}


