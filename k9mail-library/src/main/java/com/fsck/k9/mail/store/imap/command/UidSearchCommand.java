package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.Commands;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.response.SearchResponse;


public class UidSearchCommand extends SelectByIdCommand {

    private MessageRetrievalListener<ImapMessage> listener;

    private String queryString;
    private String messageId;
    private boolean performFullTextSearch;
    private Date since;
    private Set<Flag> requiredFlags;
    private Set<Flag> forbiddenFlags;

    private UidSearchCommand(ImapCommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    public String createCommandString() {
        StringBuilder builder = new StringBuilder(Commands.UID_SEARCH).append(" ");
        super.addIds(builder);
        addQueryString(builder);
        addMessageId(builder);
        addSince(builder);
        addFlags(builder);
        return builder.toString().trim();
    }

    @Override
    public SearchResponse execute() throws MessagingException {

        try {
            List<List<ImapResponse>> responses = executeInternal(false);
            SearchResponse response = SearchResponse.parse(commandFactory, responses);
            folder.handleUntaggedResponses(response);
            return response;
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(commandFactory.getConnection(), ioe);
        }

    }

    private void addQueryString(StringBuilder builder) {
        if (queryString != null) {
            String encodedQuery = ImapUtility.encodeString(queryString);
            if (performFullTextSearch) {
                builder.append("TEXT ");
            } else {
                builder.append("OR SUBJECT ").append(encodedQuery).append(" FROM ");
            }
            builder.append(encodedQuery).append(" ");
        }
    }

    private void addMessageId(StringBuilder builder) {
        if (messageId != null) {
            String encodedMessageId = ImapUtility.encodeString(messageId);
            builder.append("HEADER MESSAGE-ID ").append(encodedMessageId).append(" ");
        }
    }

    private void addSince(StringBuilder builder) {
        if (since != null) {
            SimpleDateFormat rfc3501DateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
            builder.append("SINCE ").append(rfc3501DateFormat.format(since)).append(" ");
        }
    }

    private void addFlags(StringBuilder builder) {

        if (requiredFlags != null) {
            for (Flag flag : requiredFlags) {
                builder.append(flag.getImapString()).append(" ");
            }
        }

        if (forbiddenFlags != null) {
            for (Flag flag : forbiddenFlags) {
                builder.append("NOT ").append(flag.getImapString()).append(" ");
            }
        }

    }

    @Override
    Builder newBuilder() {
        return new Builder(commandFactory, folder, listener)
                .useUids(useUids)
                .idSet(idSet)
                .idRanges(idRanges)
                .queryString(queryString)
                .messageId(messageId)
                .performFullTextSearch(performFullTextSearch)
                .since(since)
                .requiredFlags(requiredFlags)
                .forbiddenFlags(forbiddenFlags);
    }

    public static class Builder extends SelectByIdCommand.Builder<UidSearchCommand, Builder> {

        public Builder(ImapCommandFactory commandFactory, ImapFolder folder,
                MessageRetrievalListener<ImapMessage> listener) {
            super(commandFactory, folder);
            command.listener = listener;
        }

        public Builder queryString(String queryString) {
            command.queryString = queryString;
            return builder;
        }

        public Builder messageId(String messageId) {
            command.messageId = messageId;
            return builder;
        }

        public Builder performFullTextSearch(boolean performFullTextSearch) {
            command.performFullTextSearch = performFullTextSearch;
            return builder;
        }

        public Builder since(Date since) {
            command.since = since;
            return builder;
        }

        public Builder requiredFlags(Set<Flag> requiredFlags) {
            command.requiredFlags = requiredFlags;
            return builder;
        }

        public Builder forbiddenFlags(Set<Flag> forbiddenFlags) {
            command.forbiddenFlags = forbiddenFlags;
            return builder;
        }

        @Override
        UidSearchCommand createCommand() {
            return new UidSearchCommand(null);
        }

        @Override
        Builder createBuilder() {
            return this;
        }

    }

}
