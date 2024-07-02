package com.fsck.k9.storage.migrations;

import com.fsck.k9.mail.Flag;

import java.util.List;

class LegacyPendingSetFlag extends LegacyPendingCommand {
    public String folder;
    public boolean newState;
    public Flag flag;
    public List<String> uids;
}
