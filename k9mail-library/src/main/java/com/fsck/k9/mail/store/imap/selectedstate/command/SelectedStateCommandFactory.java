package com.fsck.k9.mail.store.imap.selectedstate.command;

import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;

public class SelectedStateCommandFactory {

    private ImapConnection connection;
    private ImapFolder folder;

    private SelectedStateCommandFactory(ImapConnection connection, ImapFolder folder) {
        this.connection = connection;
        this.folder = folder;
    }

    public static SelectedStateCommandFactory create(ImapConnection connection, ImapFolder folder) {
        return new SelectedStateCommandFactory(connection, folder);
    }

    public UidSearchCommand.Builder createUidSearchCommandBuilder() {
        return new UidSearchCommand.Builder(connection, folder);
    }

    public UidStoreCommand.Builder createUidStoreCommandBuilder() {
        return new UidStoreCommand.Builder(connection, folder);
    }

    public UidFetchCommand.Builder createUidFetchCommandBuilder() {
        return new UidFetchCommand.Builder(connection, folder);
    }

    public UidCopyCommand.Builder createUidCopyCommandBuilder() {
        return new UidCopyCommand.Builder(connection, folder);
    }

}
