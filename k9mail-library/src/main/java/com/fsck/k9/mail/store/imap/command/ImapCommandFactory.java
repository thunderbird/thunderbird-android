package com.fsck.k9.mail.store.imap.command;

import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;

public class ImapCommandFactory {

    private ImapConnection connection;
    private int nextCommandTag;
    private String logId;

    private ImapCommandFactory(ImapConnection connection, String logId) {
        this.connection = connection;
        this.logId = logId;
        nextCommandTag = 1;
    }

    public static ImapCommandFactory create(ImapConnection connection, String logId) {
        return new ImapCommandFactory(connection, logId);
    }

    public String getLogId() {
        return logId;
    }

    public CapabilityCommand createCapabilityCommand() {
        return new CapabilityCommand(getNextCommandTag(), this);
    }

    public UidSearchCommand.Builder createUidSearchCommandBuilder(ImapFolder folder,
                                                                  MessageRetrievalListener<ImapMessage> listener) {
        return new UidSearchCommand.Builder(getNextCommandTag(), this, folder, listener);
    }

    int getNextCommandTag() {
        return nextCommandTag++;
    }

    ImapConnection getConnection() {
        return connection;
    }

}
