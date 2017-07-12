package com.fsck.k9.mail.store.imap;


import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;


class SelectOrExamineCommand {

    private static final long INVALID_UIDVALIDITY = -1L;
    private static final long INVALID_HIGHESTMODSEQ = -1L;
    private static final long INVALID_SMALLEST_UID = -1L;

    private int mode;
    private String escapedFolderName;
    private boolean useCondstore;
    private long cachedUidValidity;
    private long cachedHighestModSeq;
    private long smallestUid;

    static SelectOrExamineCommand createNormal(int mode, String escapedFolderName) {
        return new SelectOrExamineCommand(mode, escapedFolderName, false, INVALID_UIDVALIDITY, INVALID_HIGHESTMODSEQ,
                INVALID_SMALLEST_UID);
    }

    static SelectOrExamineCommand createWithCondstoreParameter(int mode, String escapedFolderName) {
        return new SelectOrExamineCommand(mode, escapedFolderName, true, INVALID_UIDVALIDITY, INVALID_HIGHESTMODSEQ,
                INVALID_SMALLEST_UID);
    }

    static SelectOrExamineCommand createWithQresyncParameter(int mode, String escapedFolderName,
            long cachedUidValidity, long cachedHighestModSeq, long smallestUid) {
        return new SelectOrExamineCommand(mode, escapedFolderName, false, cachedUidValidity, cachedHighestModSeq,
                smallestUid);
    }

    private SelectOrExamineCommand(int mode, String escapedFolderName, boolean useCondstore, long cachedUidValidity,
            long cachedHighestModSeq, long smallestUid) {
        this.mode = mode;
        this.escapedFolderName = escapedFolderName;
        this.useCondstore = useCondstore;
        this.cachedUidValidity = cachedUidValidity;
        this.cachedHighestModSeq = cachedHighestModSeq;
        this.smallestUid = smallestUid;
    }

    String createCommandString() {
        StringBuilder builder = new StringBuilder();
        String openCommand = mode == OPEN_MODE_RW ? "SELECT" : "EXAMINE";
        builder.append(String.format("%s %s", openCommand, escapedFolderName));
        if (useQresync()) {
            String uidsParam = smallestUid == 0 ? "" : String.format(" %s:*", String.valueOf(smallestUid));
            builder.append(String.format(" (%s (%s %s%s))", Capabilities.QRESYNC, cachedUidValidity,
                    cachedHighestModSeq, uidsParam));
        } else if (useCondstore) {
            builder.append(String.format(" (%s)", Capabilities.CONDSTORE));
        }
        return builder.toString();
    }

    private boolean useQresync() {
        return cachedUidValidity != INVALID_UIDVALIDITY && cachedHighestModSeq != INVALID_HIGHESTMODSEQ;
    }
}
