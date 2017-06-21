package com.fsck.k9.mail.store.imap.command;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.response.StoreResponse;


public class UidStoreCommand extends SelectByIdCommand {

    private boolean value;
    private Set<Flag> flagSet;
    private boolean canCreateKeywords;

    private UidStoreCommand(ImapCommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    public String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_STORE).append(" ");
        super.addIds(builder);
        addValue(builder);
        addFlagSet(builder);
        return builder.toString().trim();
    }

    @Override
    public StoreResponse execute() throws MessagingException {

        try {
            List<List<ImapResponse>> responses = executeInternal(false);
            StoreResponse response = StoreResponse.parse(commandFactory, responses);
            folder.handleUntaggedResponses(response);
            return response;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(commandFactory.getConnection(), ioe);
        }

    }

    private void addValue(StringBuilder builder) {
        builder.append(value  ? "+" : "-");
    }

    private void addFlagSet(StringBuilder builder) {
        builder.append("FLAGS.SILENT (").append(combineFlags(flagSet)).append(")");
    }

    private String combineFlags(Iterable<Flag> flags) {
        List<String> flagNames = new ArrayList<String>();
        for (Flag flag : flags) {
            if (flag == Flag.SEEN) {
                flagNames.add("\\Seen");
            } else if (flag == Flag.DELETED) {
                flagNames.add("\\Deleted");
            } else if (flag == Flag.ANSWERED) {
                flagNames.add("\\Answered");
            } else if (flag == Flag.FLAGGED) {
                flagNames.add("\\Flagged");
            } else if (flag == Flag.FORWARDED && canCreateKeywords) {
                flagNames.add("$Forwarded");
            }
        }

        return ImapUtility.join(" ", flagNames);
    }

    @Override
    Builder newBuilder() {
        return new Builder(commandFactory, folder)
                .useUids(useUids)
                .idSet(idSet)
                .idRanges(idRanges)
                .value(value)
                .flagSet(flagSet)
                .canCreateKeywords(canCreateKeywords);
    }

    public static class Builder extends SelectByIdCommand.Builder<UidStoreCommand, Builder> {

        public Builder(ImapCommandFactory commandFactory, ImapFolder folder) {
            super(commandFactory, folder);
        }

        public Builder value(boolean value) {
            command.value = value;
            return builder;
        }

        public Builder flagSet(Set<Flag> flagSet) {
            command.flagSet = flagSet;
            return builder;
        }

        public Builder canCreateKeywords(boolean canCreateeywords) {
            command.canCreateKeywords = canCreateeywords;
            return builder;
        }

        @Override
        UidStoreCommand createCommand() {
            return new UidStoreCommand(null);
        }

        @Override
        Builder createBuilder() {
            return this;
        }

    }
}
