package com.fsck.k9.controller;


import java.util.List;
import java.util.Map;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;

import static com.fsck.k9.controller.Preconditions.requireValidUids;
import static com.fsck.k9.helper.Preconditions.checkNotNull;


public class MessagingControllerCommands {
    static final String COMMAND_APPEND = "append";
    static final String COMMAND_MARK_ALL_AS_READ = "mark_all_as_read";
    static final String COMMAND_SET_FLAG = "set_flag";
    static final String COMMAND_DELETE = "delete";
    static final String COMMAND_EXPUNGE = "expunge";
    static final String COMMAND_MOVE_OR_COPY = "move_or_copy";
    static final String COMMAND_MOVE_AND_MARK_AS_READ = "move_and_mark_as_read";
    static final String COMMAND_EMPTY_TRASH = "empty_trash";

    public abstract static class PendingCommand {
        public long databaseId;


        PendingCommand() { }

        public abstract String getCommandName();
        public abstract void execute(MessagingController controller, Account account) throws MessagingException;
    }

    public static class PendingMoveOrCopy extends PendingCommand {
        public final String srcFolder;
        public final String destFolder;
        public final boolean isCopy;
        public final List<String> uids;
        public final Map<String, String> newUidMap;


        public static PendingMoveOrCopy create(String srcFolder, String destFolder, boolean isCopy,
                Map<String, String> uidMap) {
            checkNotNull(srcFolder);
            checkNotNull(destFolder);
            requireValidUids(uidMap);
            return new PendingMoveOrCopy(srcFolder, destFolder, isCopy, null, uidMap);
        }

        public static PendingMoveOrCopy createLegacy(String srcFolder, String destFolder, boolean isCopy, List<String> uids) {
            return new PendingMoveOrCopy(srcFolder, destFolder, isCopy, uids, null);
        }

        private PendingMoveOrCopy(String srcFolder, String destFolder, boolean isCopy, List<String> uids,
                Map<String, String> newUidMap) {
            this.srcFolder = srcFolder;
            this.destFolder = destFolder;
            this.isCopy = isCopy;
            this.uids = uids;
            this.newUidMap = newUidMap;
        }

        @Override
        public String getCommandName() {
            return COMMAND_MOVE_OR_COPY;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingMoveOrCopy(this, account);
        }
    }

    public static class PendingMoveAndMarkAsRead extends PendingCommand {
        public final String srcFolder;
        public final String destFolder;
        public final Map<String, String> newUidMap;


        public static PendingMoveAndMarkAsRead create(String srcFolder, String destFolder, Map<String, String> uidMap) {
            checkNotNull(srcFolder);
            checkNotNull(destFolder);
            requireValidUids(uidMap);
            return new PendingMoveAndMarkAsRead(srcFolder, destFolder, uidMap);
        }

        private PendingMoveAndMarkAsRead(String srcFolder, String destFolder, Map<String, String> newUidMap) {
            this.srcFolder = srcFolder;
            this.destFolder = destFolder;
            this.newUidMap = newUidMap;
        }

        @Override
        public String getCommandName() {
            return COMMAND_MOVE_AND_MARK_AS_READ;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingMoveAndRead(this, account);
        }
    }

    public static class PendingEmptyTrash extends PendingCommand {
        public static PendingEmptyTrash create() {
            return new PendingEmptyTrash();
        }

        @Override
        public String getCommandName() {
            return COMMAND_EMPTY_TRASH;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingEmptyTrash(account);
        }
    }

    public static class PendingSetFlag extends PendingCommand {
        public final long folderId;
        public final boolean newState;
        public final Flag flag;
        public final List<String> uids;


        public static PendingSetFlag create(long folderId, boolean newState, Flag flag, List<String> uids) {
            checkNotNull(flag);
            requireValidUids(uids);
            return new PendingSetFlag(folderId, newState, flag, uids);
        }

        private PendingSetFlag(long folderId, boolean newState, Flag flag, List<String> uids) {
            this.folderId = folderId;
            this.newState = newState;
            this.flag = flag;
            this.uids = uids;
        }

        @Override
        public String getCommandName() {
            return COMMAND_SET_FLAG;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingSetFlag(this, account);
        }
    }

    public static class PendingAppend extends PendingCommand {
        public final long folderId;
        public final String uid;


        public static PendingAppend create(long folderId, String uid) {
            checkNotNull(uid);
            return new PendingAppend(folderId, uid);
        }

        private PendingAppend(long folderId, String uid) {
            this.folderId = folderId;
            this.uid = uid;
        }

        @Override
        public String getCommandName() {
            return COMMAND_APPEND;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingAppend(this, account);
        }
    }

    public static class PendingMarkAllAsRead extends PendingCommand {
        public final long folderId;


        public static PendingMarkAllAsRead create(long folderId) {
            return new PendingMarkAllAsRead(folderId);
        }

        private PendingMarkAllAsRead(long folderId) {
            this.folderId = folderId;
        }

        @Override
        public String getCommandName() {
            return COMMAND_MARK_ALL_AS_READ;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingMarkAllAsRead(this, account);
        }
    }

    public static class PendingDelete extends PendingCommand {
        public final long folderId;
        public final List<String> uids;


        public static PendingDelete create(long folderId, List<String> uids) {
            requireValidUids(uids);
            return new PendingDelete(folderId, uids);
        }

        private PendingDelete(long folderId, List<String> uids) {
            this.folderId = folderId;
            this.uids = uids;
        }

        @Override
        public String getCommandName() {
            return COMMAND_DELETE;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingDelete(this, account);
        }
    }

    public static class PendingExpunge extends PendingCommand {
        public final long folderId;


        public static PendingExpunge create(long folderId) {
            return new PendingExpunge(folderId);
        }

        private PendingExpunge(long folderId) {
            this.folderId = folderId;
        }

        @Override
        public String getCommandName() {
            return COMMAND_EXPUNGE;
        }

        @Override
        public void execute(MessagingController controller, Account account) throws MessagingException {
            controller.processPendingExpunge(this, account);
        }
    }
}
