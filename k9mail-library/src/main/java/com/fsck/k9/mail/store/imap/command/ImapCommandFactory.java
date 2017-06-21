package com.fsck.k9.mail.store.imap.command;

import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;

public class ImapCommandFactory {

    private ImapConnection connection;
    private String logId;

    private ImapCommandFactory(ImapConnection connection, String logId) {
        this.connection = connection;
        this.logId = logId;
    }

    public static ImapCommandFactory create(ImapConnection connection, String logId) {
        return new ImapCommandFactory(connection, logId);
    }

    public String getLogId() {
        return logId;
    }

    public CapabilityCommand createCapabilityCommand() {
        return new CapabilityCommand(this);
    }

    public UidSearchCommand.Builder createUidSearchCommandBuilder(ImapFolder folder,
                                                                  MessageRetrievalListener<ImapMessage> listener) {
        return new UidSearchCommand.Builder(this, folder, listener);
    }

    public UidStoreCommand.Builder createUidStoreCommandBuilder(ImapFolder folder) {
        return new UidStoreCommand.Builder(this, folder);
    }

    ImapConnection getConnection() {
        return connection;
    }

}
