package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import android.text.TextUtils;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.response.SearchResponse;


public class UidSearchCommand extends BaseCommand {

    private ImapFolder folder;
    private MessageRetrievalListener<ImapMessage> listener;

    private boolean useUids;
    private Set<Long> idSet;
    private List<Range> idRanges;
    private String queryString;
    private String messageId;
    private boolean performFullTextSearch;
    private Date since;
    private Set<Flag> requiredFlags;
    private Set<Flag> forbiddenFlags;

    private UidSearchCommand(Builder builder) {
        super(builder.tag, builder.commandFactory);
        this.folder = builder.folder;
        this.listener = builder.listener;
        this.useUids = builder.useUids;
        this.idSet = builder.idSet;
        this.idRanges = builder.idRanges;
        this.queryString = builder.queryString;
        this.messageId = builder.messageId;
        this.performFullTextSearch = builder.performFullTextSearch;
        this.since = builder.since;
        this.requiredFlags = builder.requiredFlags;
        this.forbiddenFlags = builder.forbiddenFlags;
    }

    @Override
    public String createCommandString() {

        StringBuilder builder = new StringBuilder(String.format(Locale.US, "%d UID SEARCH ", tag));
        addIds(builder);
        addQueryString(builder);
        addMessageId(builder);
        addSince(builder);
        addFlags(builder);
        return builder.toString().trim();
    }

    @Override
    public List<UidSearchCommand> splitCommand(int lengthLimit) {

        List<UidSearchCommand> commands = new ArrayList<>();

        if (idSet != null || idRanges != null) {

            while ((idSet != null && !idSet.isEmpty()) || (idRanges != null && !idRanges.isEmpty())) {

                Builder builder = this.newBuilder()
                        .tag(commandFactory.getNextCommandTag())
                        .idSet(null)
                        .idRanges(null);

                int length = builder.build().createCommandString().length();
                while (length < lengthLimit) {

                    if (idSet != null && !idSet.isEmpty()) {

                        Long first = idSet.iterator().next();
                        length += (String.valueOf(first).length() + 1);
                        if (length < lengthLimit) {
                            builder.addId(first);
                            idSet.remove(first);
                        } else {
                            break;
                        }

                    } else if (idRanges != null && !idRanges.isEmpty()) {

                        Range first = idRanges.iterator().next();
                        length += (first.toString().length() + 1);
                        if (length < lengthLimit) {
                            builder.addIdRange(first.getStart(), first.getEnd());
                            idRanges.remove(first);
                        } else {
                            break;
                        }
                    }
                }
                commands.add(builder.build());
            }

        } else {
            //This should never happen
            commands = Collections.singletonList(this);
        }

        return commands;

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

    private void addIds(StringBuilder builder) {

        if (idSet != null || idRanges != null) {
            if (useUids) {
                builder.append("UID ");
            }

            optimizeGroupings();

            if (idSet != null) {
                builder.append(TextUtils.join(",", idSet));
            }
            if (idRanges != null) {
                builder.append(",").append(TextUtils.join(",", idRanges));
            }
            builder.append(" ");
        }
    }

    private void optimizeGroupings() {

        if (idRanges != null && idRanges.get(0).end == Range.LAST_ID) {
            return;
        }

        TreeSet<Long> fullIdSet = new TreeSet<>();
        if (idSet != null) {
            fullIdSet.addAll(idSet);
        }
        if (idRanges != null) {
            for (Range numberRange : idRanges) {
                for (long i = numberRange.getStart();i <= numberRange.getEnd();i++) {
                    fullIdSet.add(i);
                }
            }
        }

        Builder builder = this.newBuilder()
                .idSet(null)
                .idRanges(null);
        List<Long> idList = new ArrayList<>(fullIdSet);
        int start = 0;

        for (int i = 1; i < idList.size();i++) {
            if (idList.get(i - 1) + 1 != idList.get(i)) {
                checkAndAddIds(builder, idList, start, i - 1);
                start = i;
            }
        }
        checkAndAddIds(builder, idList, start, idList.size() - 1);

        this.idSet = builder.idSet;
        this.idRanges = builder.idRanges;
    }

    private void checkAndAddIds(Builder builder, List<Long> idList, int start, int end) {
        if (start == end) {
            builder.addId(idList.get(start));
        } else {
            builder.addIdRange(idList.get(start), idList.get(end));
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
                builder.append(flag.getRequiredImapString()).append(" ");
            }
        }

        if (forbiddenFlags != null) {
            for (Flag flag : forbiddenFlags) {
                builder.append(flag.getForbiddenImapString()).append(" ");
            }
        }

    }

    private Builder newBuilder() {
        return new Builder(tag, commandFactory, folder, listener)
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

    public static class Builder {

        private int tag;
        private ImapCommandFactory commandFactory;
        private ImapFolder folder;
        private MessageRetrievalListener<ImapMessage> listener;

        private boolean useUids;
        private Set<Long> idSet;
        private List<Range> idRanges;
        private String queryString;
        private String messageId;
        private boolean performFullTextSearch;
        private Date since;
        private Set<Flag> requiredFlags;
        private Set<Flag> forbiddenFlags;

        public Builder(int tag, ImapCommandFactory commandFactory, ImapFolder folder,
                       MessageRetrievalListener<ImapMessage> listener) {
            this.tag = tag;
            this.commandFactory = commandFactory;
            this.folder = folder;
            this.listener = listener;
        }

        public Builder tag(int tag) {
            this.tag = tag;
            return this;
        }

        public Builder useUids(boolean useUids) {
            this.useUids = useUids;
            return this;
        }

        public Builder idSet(Collection<Long> idSet) {
            if (idSet != null) {
                this.idSet = new HashSet<>(idSet);
            } else {
                this.idSet = null;
            }
            return this;
        }

        Builder addId(Long id) {
            if (idSet == null) {
                idSet = new HashSet<>();
            }
            idSet.add(id);
            return this;
        }

        Builder idRanges(List<Range> idRanges) {
            this.idRanges = idRanges;
            return this;
        }

        public Builder addIdRange(Long start, Long end) {
            if (idRanges == null) {
                idRanges = new ArrayList<>();
            }
            idRanges.add(new Range(start, end));
            return this;
        }

        public Builder allIds(boolean allIds) {
            if (allIds) {
                idSet = null;
                idRanges = Collections.singletonList(new Range(Range.FIRST_ID, Range.LAST_ID));
            }
            return this;
        }

        public Builder onlyHighestId(boolean onlyHighestId) {
            if (onlyHighestId) {
                idSet = null;
                idRanges = Collections.singletonList(new Range(Range.LAST_ID, Range.LAST_ID));
            }
            return this;
        }

        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }

        public Builder messageId(String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder performFullTextSearch(boolean performFullTextSearch) {
            this.performFullTextSearch = performFullTextSearch;
            return this;
        }

        public Builder since(Date since) {
            this.since = since;
            return this;
        }

        public Builder requiredFlags(Set<Flag> requiredFlags) {
            this.requiredFlags = requiredFlags;
            return this;
        }

        public Builder forbiddenFlags(Set<Flag> forbiddenFlags) {
            this.forbiddenFlags = forbiddenFlags;
            return this;
        }

        public UidSearchCommand build() {
            return new UidSearchCommand(this);
        }

    }

    public static class Range {

        private static final long FIRST_ID = 1L;
        private static final long LAST_ID = Long.MAX_VALUE;

        private Long start;
        private Long end;

        Range(Long start, Long end) {
            if (start <= end) {
                this.start = start;
                this.end = end;
            } else {
                this.start = end;
                this.end = start;
            }
        }

        public Long getStart() {
            return start;
        }

        public Long getEnd() {
            return end;
        }

        @Override
        public String toString() {
            if (start == LAST_ID && end == LAST_ID) {
                return "*:*";
            }

            if (start != LAST_ID && end == LAST_ID) {
                return start + ":" + "*";
            }

            return start + ":" + end;
        }

    }

}
