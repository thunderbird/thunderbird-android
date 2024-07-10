package com.fsck.k9.storage.migrations;

import java.util.Map;

class LegacyPendingMoveAndMarkAsRead extends LegacyPendingCommand {
    public String srcFolder;
    public String destFolder;
    public Map<String, String> newUidMap;
}
