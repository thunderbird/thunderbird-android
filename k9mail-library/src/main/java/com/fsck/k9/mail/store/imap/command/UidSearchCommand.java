package com.fsck.k9.mail.store.imap.command;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

    private Set<Long> sequenceSet;
    private List<Map.Entry<String, String>> sequenceRanges;
    private Set<Long> uidSet;
    private List<Map.Entry<String, String>> uidRanges;
    private String queryString;
    private boolean performFullTextSearch;
    private Date since;
    private Set<Flag> requiredFlags;
    private Set<Flag> forbiddenFlags;

    private UidSearchCommand(Builder builder) {
        super(builder.tag, builder.commandFactory);
        this.folder = builder.folder;
        this.listener = builder.listener;
        this.sequenceSet = builder.sequenceSet;
        this.sequenceRanges = builder.sequenceRanges;
        this.uidSet = builder.uidSet;
        this.uidRanges = builder.uidRanges;
        this.queryString = builder.queryString;
        this.performFullTextSearch = builder.performFullTextSearch;
        this.since = builder.since;
        this.requiredFlags = builder.requiredFlags;
        this.forbiddenFlags = builder.forbiddenFlags;
    }

    @Override
    public String createCommandString() {

        StringBuilder builder = new StringBuilder(String.format(Locale.US, "%d UID SEARCH ", tag));
        addSequenceNumbers(builder);
        addUids(builder);
        addQueryString(builder);
        addSince(builder);
        addRequiredFlags(builder);
        addForbiddenFlags(builder);
        if (builder.lastIndexOf(" ") == builder.length() - 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    @Override
    public List<UidSearchCommand> splitCommand(int lengthLimit) {

        List<UidSearchCommand> commands = new ArrayList<>();

        Set<Long> numberSet = null;
        List<Map.Entry<String, String>> numberRanges = null;
        boolean splittingUids = false;

        //Currently all usages search on the basis of either sequence numbers or uids, but not both
        if (sequenceSet != null || sequenceRanges != null) {
            numberSet = sequenceSet;
            numberRanges = sequenceRanges;
            splittingUids = false;
        } else if (uidSet != null || uidRanges != null) {
            numberSet = uidSet;
            numberRanges = uidRanges;
            splittingUids = true;
        }

        if (numberSet != null || numberRanges != null) {

            while ((numberSet != null && !numberSet.isEmpty()) || (numberRanges != null && !numberRanges.isEmpty())) {

                UidSearchCommand.Builder builder = this.newBuilder()
                        .sequenceSet(null)
                        .sequenceRanges(null)
                        .uidSet(null)
                        .uidRanges(null);

                int length = builder.getLength();
                while (length < lengthLimit) {

                    if (numberSet != null && !numberSet.isEmpty()) {

                        Long first = numberSet.iterator().next();
                        if (splittingUids) {
                            builder.addUid(first);
                        } else {
                            builder.addSequenceNumber(first);
                        }
                        numberSet.remove(first);

                    } else if (numberRanges != null && !numberRanges.isEmpty()) {

                        Entry<String, String> first = numberRanges.iterator().next();
                        if (splittingUids) {
                            builder.addUidRange(first.getKey(), first.getValue());
                        } else {
                            builder.addSequenceRange(first.getKey(), first.getValue());
                        }
                        numberRanges.remove(first);

                    }
                    length = builder.getLength();
                }
                commands.add(builder.build());
            }

        } else {
            //This should never happen
            commands = Collections.singletonList(this);
        }

        return commands;

    }

    public SearchResponse execute() throws MessagingException {

        try {
            List<List<ImapResponse>> responses = executeInternal(false);
            return SearchResponse.parseMultiple(commandFactory, responses);
        } catch (IOException ioe) {
            throw folder.ioExceptionHandler(commandFactory.getConnection(), ioe);
        }

    }

    private void addSequenceNumbers(StringBuilder builder) {
        addGrouping(builder, sequenceSet, sequenceRanges);
    }

    private void addUids(StringBuilder builder) {
        if (uidSet != null || uidRanges != null) {
            builder.append("UID ");
            addGrouping(builder, uidSet, uidRanges);
        }
    }

    private void addGrouping(StringBuilder builder, Set<Long> numberSet,
                             List<Map.Entry<String, String>> numberRanges) {
        optimizeGrouping(numberSet, numberRanges);
        List<String> sequenceElements = new ArrayList<>();
        if (numberSet != null) {
            for (Long number : numberSet) {
                sequenceElements.add(String.valueOf(number));
            }
        }
        if (numberRanges != null) {
            for (Map.Entry<String, String> numberRange : numberRanges) {
                sequenceElements.add(numberRange.getKey() + ":" + numberRange.getValue());
            }
        }
        if (sequenceElements.size() > 0) {
            builder.append(TextUtils.join(",", sequenceElements.toArray()));
            builder.append(" ");
        }
    }

    private void optimizeGrouping(Set<Long> numberSet, List<Map.Entry<String, String>> numberRanges) {
        //TODO minimize the number of elements in both the lists
    }

    private void addQueryString(StringBuilder builder) {
        //TODO use a literal string instead of a quoted string
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

    private void addSince(StringBuilder builder) {
        if (since != null) {
            SimpleDateFormat rfc3501DateFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.US);
            builder.append("SINCE ").append(rfc3501DateFormat.format(since)).append(" ");
        }
    }

    private void addRequiredFlags(StringBuilder builder) {
        if (requiredFlags != null) {
            for (Flag flag : requiredFlags) {
                switch (flag) {
                    case DELETED: {
                        builder.append("DELETED ");
                        break;
                    }
                    case SEEN: {
                        builder.append("SEEN ");
                        break;
                    }
                    case ANSWERED: {
                        builder.append("ANSWERED ");
                        break;
                    }
                    case FLAGGED: {
                        builder.append("FLAGGED ");
                        break;
                    }
                    case DRAFT: {
                        builder.append("DRAFT ");
                        break;
                    }
                    case RECENT: {
                        builder.append("RECENT ");
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    private void addForbiddenFlags(StringBuilder builder) {
        if (forbiddenFlags != null) {
            for (Flag flag : forbiddenFlags) {
                switch (flag) {
                    case DELETED: {
                        builder.append("UNDELETED ");
                        break;
                    }
                    case SEEN: {
                        builder.append("UNSEEN ");
                        break;
                    }
                    case ANSWERED: {
                        builder.append("UNANSWERED ");
                        break;
                    }
                    case FLAGGED: {
                        builder.append("UNFLAGGED ");
                        break;
                    }
                    case DRAFT: {
                        builder.append("UNDRAFT ");
                        break;
                    }
                    case RECENT: {
                        builder.append("UNRECENT ");
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }
    }

    private Builder newBuilder() {
        return new Builder(tag, commandFactory, folder, listener)
                .sequenceSet(sequenceSet)
                .sequenceRanges(sequenceRanges)
                .uidSet(uidSet)
                .uidRanges(uidRanges)
                .queryString(queryString)
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

        private Set<Long> sequenceSet;
        private List<Map.Entry<String, String>> sequenceRanges;
        private Set<Long> uidSet;
        private List<Map.Entry<String, String>> uidRanges;
        private String queryString;
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

        public Builder sequenceSet(Collection<Long> sequenceSet) {
            this.sequenceSet = new HashSet<>(sequenceSet);
            return this;
        }

        public Builder addSequenceNumber(Long sequenceNumber) {
            if (sequenceSet == null) {
                sequenceSet = new HashSet<>();
            }
            sequenceSet.add(sequenceNumber);
            return this;
        }

        public Builder sequenceRanges(List<Map.Entry<String, String>> sequenceRanges) {
            this.sequenceRanges = sequenceRanges;
            return this;
        }

        public Builder addSequenceRange(String lowerBound, String upperBound) {
            if (sequenceRanges == null) {
                sequenceRanges = new ArrayList<>();
            }
            sequenceRanges.add(new AbstractMap.SimpleEntry<String, String>(lowerBound, upperBound));
            return this;
        }

        public Builder uidSet(Collection<Long> uidSet) {
            this.uidSet = new HashSet<>(uidSet);
            return this;
        }

        public Builder addUid(Long uid) {
            if (uidSet == null) {
                uidSet = new HashSet<>();
            }
            uidSet.add(uid);
            return this;
        }

        public Builder uidRanges(List<Map.Entry<String, String>> uidRanges) {
            this.uidRanges = uidRanges;
            return this;
        }

        public Builder addUidRange(String lowerBound, String upperBound) {
            if (uidRanges == null) {
                uidRanges = new ArrayList<>();
            }
            uidRanges.add(new AbstractMap.SimpleEntry<String, String>(lowerBound, upperBound));
            return this;
        }

        public Builder queryString(String queryString) {
            this.queryString = queryString;
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

        private int getLength() {
            return this.build().createCommandString().getBytes().length;
        }

    }
}
