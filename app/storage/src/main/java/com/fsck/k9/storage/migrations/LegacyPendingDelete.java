package com.fsck.k9.storage.migrations;

import java.util.List;

import static com.fsck.k9.controller.Preconditions.requireValidUids;
import static com.fsck.k9.helper.Preconditions.checkNotNull;

class LegacyPendingDelete extends LegacyPendingCommand {
    public final String folder;
    public final List<String> uids;


    static LegacyPendingDelete create(String folder, List<String> uids) {
        checkNotNull(folder);
        requireValidUids(uids);
        return new LegacyPendingDelete(folder, uids);
    }

    private LegacyPendingDelete(String folder, List<String> uids) {
        this.folder = folder;
        this.uids = uids;
    }
}
