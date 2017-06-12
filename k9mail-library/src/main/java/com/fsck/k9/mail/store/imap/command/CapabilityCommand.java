package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapResponse;


public class CapabilityCommand extends BaseCommand {

    CapabilityCommand(int tag, ImapCommandFactory commandFactory) {
        super(tag, commandFactory);
    }

    @Override
    public String createCommandString() {
        return String.format(Locale.US, "%d CAPABILITY", tag);
    }

    //This method should never be called since the command is very small
    @Override
    public List<CapabilityCommand> splitCommand(int lengthLimit) {
        return Collections.singletonList(this);
    }

    public List<ImapResponse> execute() throws IOException, MessagingException {
        return executeInternal(false).get(0);
    }

}
