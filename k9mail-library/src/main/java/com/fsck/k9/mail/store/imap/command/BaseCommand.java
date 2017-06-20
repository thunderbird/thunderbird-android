package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.response.BaseResponse;


abstract class BaseCommand {

    private static final int LENGTH_LIMIT_WITHOUT_CONDSTORE = 970;
    private static final int LENGTH_LIMIT_WITH_CONDSTORE = 8162;

    ImapCommandFactory commandFactory;

    BaseCommand(ImapCommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    abstract String createCommandString();

    abstract List<? extends BaseCommand> splitCommand(int lengthLimit);

    List<List<ImapResponse>> executeInternal(boolean sensitive) throws IOException, MessagingException {

        List<BaseCommand> commands;
        String commandString = createCommandString();
        if (commandString.length() > getCommandLengthLimit()) {
            commands = Collections.unmodifiableList(splitCommand(getCommandLengthLimit()));
        } else {
            commands = Collections.singletonList(this);
        }

        ImapConnection connection = commandFactory.getConnection();
        List<List<ImapResponse>> responses = new ArrayList<>();
        for (BaseCommand command : commands) {
            responses.add(connection.executeSimpleCommand(command.createCommandString(), sensitive));
        }
        return responses;
    }

    public abstract BaseResponse execute() throws MessagingException;


    private int getCommandLengthLimit() throws IOException, MessagingException  {
        boolean condstoreSupported = commandFactory.getConnection().isCondstoreCapable();
        if (condstoreSupported) {
            return LENGTH_LIMIT_WITH_CONDSTORE;
        } else {
            return LENGTH_LIMIT_WITHOUT_CONDSTORE;
        }
    }
}
