package com.fsck.k9.storage.migrations;


import java.util.List;

import com.fsck.k9.mail.Flag;

class LegacyPendingSetFlag extends LegacyPendingCommand {
    public String folder;
    public boolean newState;
    public Flag flag;
    public List<String> uids;
}
