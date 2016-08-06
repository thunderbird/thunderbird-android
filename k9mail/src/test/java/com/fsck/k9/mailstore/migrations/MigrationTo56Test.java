package com.fsck.k9.mailstore.migrations;


import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.controller.MessagingControllerCommands.PendingAppend;
import com.fsck.k9.controller.MessagingControllerCommands.PendingCommand;
import com.fsck.k9.controller.MessagingControllerCommands.PendingEmptyTrash;
import com.fsck.k9.controller.MessagingControllerCommands.PendingExpunge;
import com.fsck.k9.controller.MessagingControllerCommands.PendingMarkAllAsRead;
import com.fsck.k9.controller.MessagingControllerCommands.PendingMoveOrCopy;
import com.fsck.k9.controller.MessagingControllerCommands.PendingSetFlag;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mailstore.migrations.MigrationTo56.OldPendingCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class MigrationTo56Test {
    static final String PENDING_COMMAND_MOVE_OR_COPY = "com.fsck.k9.MessagingController.moveOrCopy";
    static final String PENDING_COMMAND_MOVE_OR_COPY_BULK = "com.fsck.k9.MessagingController.moveOrCopyBulk";
    static final String PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW = "com.fsck.k9.MessagingController.moveOrCopyBulkNew";
    static final String PENDING_COMMAND_EMPTY_TRASH = "com.fsck.k9.MessagingController.emptyTrash";
    static final String PENDING_COMMAND_SET_FLAG_BULK = "com.fsck.k9.MessagingController.setFlagBulk";
    static final String PENDING_COMMAND_SET_FLAG = "com.fsck.k9.MessagingController.setFlag";
    static final String PENDING_COMMAND_APPEND = "com.fsck.k9.MessagingController.append";
    static final String PENDING_COMMAND_MARK_ALL_AS_READ = "com.fsck.k9.MessagingController.markAllAsRead";
    static final String PENDING_COMMAND_EXPUNGE = "com.fsck.k9.MessagingController.expunge";

    static final String SOURCE_FOLDER = "source_folder";
    static final String DEST_FOLDER = "dest_folder";
    static final boolean IS_COPY = true;
    static final String[] UID_ARRAY = new String[] { "uid_1", "uid_2" };
    static final boolean FLAG_STATE = true;
    static final Flag FLAG = Flag.X_DESTROYED;
    static final String UID = "uid";
    static final HashMap<String, String> UID_MAP = new HashMap<>();
    static {
        UID_MAP.put("uid_1", "uid_other_1");
        UID_MAP.put("uid_2", "uid_other_2");
    }


    @Test
    public void testMigrateMoveOrCopy__withUidArray() {
        OldPendingCommand command = queueMoveOrCopy(SOURCE_FOLDER, DEST_FOLDER, IS_COPY, UID_ARRAY);

        PendingMoveOrCopy pendingCommand = (PendingMoveOrCopy) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.srcFolder);
        assertEquals(DEST_FOLDER, pendingCommand.destFolder);
        assertEquals(IS_COPY, pendingCommand.isCopy);
        assertEquals(Arrays.asList(UID_ARRAY), pendingCommand.uids);
        assertNull(pendingCommand.newUidMap);
    }

    OldPendingCommand queueMoveOrCopy(String srcFolder, String destFolder, boolean isCopy, String uids[]) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW;

        int length = 4 + uids.length;
        command.arguments = new String[length];
        command.arguments[0] = srcFolder;
        command.arguments[1] = destFolder;
        command.arguments[2] = Boolean.toString(isCopy);
        command.arguments[3] = Boolean.toString(false);
        System.arraycopy(uids, 0, command.arguments, 4, uids.length);
        return command;
    }


    @Test
    public void testMigrateMoveOrCopy__withUidMap() {
        OldPendingCommand command = queueMoveOrCopy(SOURCE_FOLDER, DEST_FOLDER, IS_COPY, UID_MAP);

        PendingMoveOrCopy pendingCommand = (PendingMoveOrCopy) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.srcFolder);
        assertEquals(DEST_FOLDER, pendingCommand.destFolder);
        assertEquals(IS_COPY, pendingCommand.isCopy);
        assertEquals(UID_MAP, pendingCommand.newUidMap);
        assertNull(pendingCommand.uids);
    }

    OldPendingCommand queueMoveOrCopy(
            String srcFolder, String destFolder, boolean isCopy, Map<String, String> uidMap) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK_NEW;

        int length = 4 + uidMap.keySet().size() + uidMap.values().size();
        command.arguments = new String[length];
        command.arguments[0] = srcFolder;
        command.arguments[1] = destFolder;
        command.arguments[2] = Boolean.toString(isCopy);
        command.arguments[3] = Boolean.toString(true);
        Set<String> strings = uidMap.keySet();
        System.arraycopy(strings.toArray(new String[strings.size()]), 0, command.arguments, 4, uidMap.keySet().size());
        Collection<String> values = uidMap.values();
        System.arraycopy(values.toArray(new String[values.size()]), 0, command.arguments, 4 + uidMap.keySet().size(), uidMap.values().size());
        return command;
    }


    @Test
    public void testMigrateMoveOrCopy__withOldFormat() {
        OldPendingCommand command = queueMoveOrCopyOld(SOURCE_FOLDER, DEST_FOLDER, IS_COPY, UID_ARRAY);

        PendingMoveOrCopy pendingCommand = (PendingMoveOrCopy) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.srcFolder);
        assertEquals(DEST_FOLDER, pendingCommand.destFolder);
        assertEquals(IS_COPY, pendingCommand.isCopy);
        assertEquals(Arrays.asList(UID_ARRAY), pendingCommand.uids);
        assertNull(pendingCommand.newUidMap);
    }

    OldPendingCommand queueMoveOrCopyOld(String srcFolder, String destFolder, boolean isCopy, String uids[]) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY_BULK;

        int length = 3 + uids.length;
        command.arguments = new String[length];
        command.arguments[0] = srcFolder;
        command.arguments[1] = destFolder;
        command.arguments[2] = Boolean.toString(isCopy);
        System.arraycopy(uids, 0, command.arguments, 3, uids.length);
        return command;
    }


    @Test
    public void testMigrateMoveOrCopy__withEvenOlderFormat() {
        OldPendingCommand command = queueMoveOrCopyEvenOlder(SOURCE_FOLDER, DEST_FOLDER, UID, IS_COPY);

        PendingMoveOrCopy pendingCommand = (PendingMoveOrCopy) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.srcFolder);
        assertEquals(DEST_FOLDER, pendingCommand.destFolder);
        assertEquals(IS_COPY, pendingCommand.isCopy);
        assertEquals(Collections.singletonList(UID), pendingCommand.uids);
        assertNull(pendingCommand.newUidMap);
    }

    OldPendingCommand queueMoveOrCopyEvenOlder(String srcFolder, String destFolder, String uid, boolean isCopy) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_MOVE_OR_COPY;

        command.arguments = new String[4];
        command.arguments[0] = srcFolder;
        command.arguments[1] = uid;
        command.arguments[2] = destFolder;
        command.arguments[3] = Boolean.toString(isCopy);
        return command;
    }


    @Test
    public void testMigrateSetFlag() {
        OldPendingCommand command = queueSetFlagBulk(SOURCE_FOLDER, FLAG_STATE, FLAG, UID_ARRAY);

        PendingSetFlag pendingCommand = (PendingSetFlag) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.folder);
        assertEquals(FLAG_STATE, pendingCommand.newState);
        assertEquals(FLAG, pendingCommand.flag);
        assertEquals(Arrays.asList(UID_ARRAY), pendingCommand.uids);
    }

    OldPendingCommand queueSetFlagBulk(String folderName, boolean newState, Flag flag, String[] uids) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_SET_FLAG_BULK;
        int length = 3 + uids.length;
        command.arguments = new String[length];
        command.arguments[0] = folderName;
        command.arguments[1] = Boolean.toString(newState);
        command.arguments[2] = flag.toString();
        System.arraycopy(uids, 0, command.arguments, 3, uids.length);
        return command;
    }


    @Test
    public void testMigrateSetFlag__oldFormat() {
        OldPendingCommand command = queueSetFlagOld(SOURCE_FOLDER, FLAG_STATE, FLAG, UID);

        PendingSetFlag pendingCommand = (PendingSetFlag) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.folder);
        assertEquals(FLAG_STATE, pendingCommand.newState);
        assertEquals(FLAG, pendingCommand.flag);
        assertEquals(Collections.singletonList(UID), pendingCommand.uids);
    }

    OldPendingCommand queueSetFlagOld(String folderName, boolean newState, Flag flag, String uid) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_SET_FLAG;
        command.arguments = new String[4];
        command.arguments[0] = folderName;
        command.arguments[1] = uid;
        command.arguments[2] = Boolean.toString(newState);
        command.arguments[3] = flag.toString();
        return command;
    }


    @Test
    public void testMigrateExpunge() {
        OldPendingCommand command = queueExpunge(SOURCE_FOLDER);

        PendingExpunge pendingCommand = (PendingExpunge) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.folder);
    }

    OldPendingCommand queueExpunge(String folderName) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_EXPUNGE;
        command.arguments = new String[1];
        command.arguments[0] = folderName;
        return command;
    }


    @Test
    public void testMigrateEmptyTrash() {
        OldPendingCommand command = queueEmptyTrash();

        PendingCommand pendingCommand = MigrationTo56.migratePendingCommand(command);

        assertTrue(pendingCommand instanceof PendingEmptyTrash);
    }

    OldPendingCommand queueEmptyTrash() {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_EMPTY_TRASH;
        command.arguments = new String[0];
        return command;
    }


    @Test
    public void testMigrateMarkAllMessagesRead() {
        OldPendingCommand command = queueMarkAllMessagesRead(SOURCE_FOLDER);

        PendingMarkAllAsRead pendingCommand = (PendingMarkAllAsRead) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.folder);
    }

    OldPendingCommand queueMarkAllMessagesRead(final String folder) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_MARK_ALL_AS_READ;
        command.arguments = new String[] { folder };
        return command;
    }


    @Test
    public void testMigrateAppend() {
        OldPendingCommand command = queueAppend(SOURCE_FOLDER, UID);

        PendingAppend pendingCommand = (PendingAppend) MigrationTo56.migratePendingCommand(command);

        assertEquals(SOURCE_FOLDER, pendingCommand.folder);
        assertEquals(UID, pendingCommand.uid);
    }

    OldPendingCommand queueAppend(String srcFolder, String uid) {
        OldPendingCommand command = new OldPendingCommand();
        command.command = PENDING_COMMAND_APPEND;
        command.arguments = new String[] { srcFolder, uid };
        return command;
    }

}