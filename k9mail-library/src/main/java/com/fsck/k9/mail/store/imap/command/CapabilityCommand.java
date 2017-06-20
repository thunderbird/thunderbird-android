package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.response.CapabilityResponse;


public class CapabilityCommand extends BaseCommand {

    CapabilityCommand(ImapCommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    String createCommandString() {
        return Commands.CAPABILITY;
    }

    //This method should never be called since the command is very small
    @Override
    List<CapabilityCommand> splitCommand(int lengthLimit) {
        return Collections.singletonList(this);
    }

    @Override
    public CapabilityResponse execute() throws MessagingException {
        //This is never used
        return null;
    }

    public List<ImapResponse> executeInternal() throws IOException, MessagingException {
        return executeInternal(false).get(0);
    }

}
