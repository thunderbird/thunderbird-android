package com.fsck.k9.storage.migrations;

import com.fsck.k9.mail.Flag;

import java.util.List;

import static com.fsck.k9.controller.Preconditions.requireValidUids;
import static com.fsck.k9.helper.Preconditions.checkNotNull;

class LegacyPendingSetFlag {
    public final String folder;
    public final boolean newState;
    public final Flag flag;
    public final List<String> uids;


    public static LegacyPendingSetFlag create(String folder, boolean newState, Flag flag, List<String> uids) {
        checkNotNull(folder);
        checkNotNull(flag);
        requireValidUids(uids);
        return new LegacyPendingSetFlag(folder, newState, flag, uids);
    }

    private LegacyPendingSetFlag(String folder, boolean newState, Flag flag, List<String> uids) {
        this.folder = folder;
        this.newState = newState;
        this.flag = flag;
        this.uids = uids;
    }
}
