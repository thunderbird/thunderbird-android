package com.fsck.k9.controller;


import com.fsck.k9.mailstore.LocalStore;

/**
 * A helper class to produce pending commands for testing MessageController.
 */
public class PendingCommandHelper {
    static LocalStore.PendingCommand append() {
        LocalStore.PendingCommand pendingCommand = new LocalStore.PendingCommand();
        pendingCommand.arguments = new String[]{"Folder", "1"};
        pendingCommand.command = "com.fsck.k9.MessagingController.append";
        return pendingCommand;
    }
    static LocalStore.PendingCommand copyBulkNew() {
        LocalStore.PendingCommand pendingCommand = new LocalStore.PendingCommand();
        pendingCommand.arguments = new String[]{"Source", "Dest", "true", "true", "srcUid", "localDestUid"};
        pendingCommand.command = "com.fsck.k9.MessagingController.moveOrCopyBulkNew";
        return pendingCommand;
    }
}
