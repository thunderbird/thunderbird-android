package com.fsck.k9.controller;


import java.util.Map;

import com.fsck.k9.mail.Flag;


public class MessagingControllerCommands {
    static final String COMMAND_APPEND = "append";
    static final String COMMAND_MARK_ALL_AS_READ = "mark_all_as_read";
    static final String COMMAND_SET_FLAG = "set_flag";
    static final String COMMAND_EXPUNGE = "expunge";
    static final String COMMAND_MOVE_OR_COPY = "move_or_copy";
    static final String COMMAND_EMPTY_TRASH = "empty_trash";


    public static PendingSetFlag createSetFlag(String folder, boolean newState, Flag flag, String[] uids) {
        return new PendingSetFlag(folder, newState, flag, uids);
    }

    public static PendingExpunge createExpunge(String folderName) {
        return new PendingExpunge(folderName);
    }

    public static PendingMarkAllAsRead createMarkAllAsRead(String folder) {
        return new PendingMarkAllAsRead(folder);
    }

    public static PendingAppend createAppend(String folderName, String uid) {
        return new PendingAppend(folderName, uid);
    }

    public static PendingEmptyTrash createEmptyTrash() {
        return new PendingEmptyTrash();
    }

    public static PendingMoveOrCopy createMoveOrCopyBulk(String srcFolder, String destFolder, boolean isCopy, Map<String, String> uidMap) {
        return new PendingMoveOrCopy(srcFolder, destFolder, isCopy, null, uidMap);
    }

    public static PendingMoveOrCopy createMoveOrCopyBulk(String srcFolder, String destFolder, boolean isCopy, String[] uids) {
        return new PendingMoveOrCopy(srcFolder, destFolder, isCopy, uids, null);
    }


    public static abstract class PendingCommand {
        public transient long databaseId;

        public abstract String getCommandName();

        private PendingCommand() { }
    }

    public static class PendingMoveOrCopy extends PendingCommand {
        public final String srcFolder;
        public final String destFolder;
        public final boolean isCopy;
        public final String[] uids;
        public final Map<String, String> newUidMap;

        public PendingMoveOrCopy(
                String srcFolder, String destFolder, boolean isCopy, String[] uids, Map<String, String> newUidMap) {
            this.srcFolder = srcFolder;
            this.destFolder = destFolder;
            this.isCopy = isCopy;
            this.uids = uids;
            this.newUidMap = newUidMap;
        }

        public String getCommandName() {
            return COMMAND_MOVE_OR_COPY;
        }
    }

    public static class PendingEmptyTrash extends PendingCommand {
        public String getCommandName() {
            return COMMAND_EMPTY_TRASH;
        }
    }

    public static class PendingSetFlag extends PendingCommand {
        public final String folder;
        public final boolean newState;
        public final Flag flag;
        public final String[] uids;

        public PendingSetFlag(String folder, boolean newState, Flag flag, String[] uids) {
            this.folder = folder;
            this.newState = newState;
            this.flag = flag;
            this.uids = uids;
        }

        public String getCommandName() {
            return COMMAND_SET_FLAG;
        }
    }

    public static class PendingAppend extends PendingCommand {
        public final String folder;
        public final String uid;

        public PendingAppend(String folder, String uid) {
            this.folder = folder;
            this.uid = uid;
        }

        public String getCommandName() {
            return COMMAND_APPEND;
        }
    }

    public static class PendingMarkAllAsRead extends PendingCommand {
        public final String folder;

        public PendingMarkAllAsRead(String folder) {
            this.folder = folder;
        }

        public String getCommandName() {
            return COMMAND_MARK_ALL_AS_READ;
        }
    }

    public static class PendingExpunge extends PendingCommand {
        public final String folder;

        public PendingExpunge(String folder) {
            this.folder = folder;
        }

        public String getCommandName() {
            return COMMAND_EXPUNGE;
        }
    }
}
