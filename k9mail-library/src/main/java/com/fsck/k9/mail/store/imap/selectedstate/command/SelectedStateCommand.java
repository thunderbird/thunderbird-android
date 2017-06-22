package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.ImapUtility;
import com.fsck.k9.mail.store.imap.selectedstate.response.SelectedStateResponse;


//This is the base class for a command that is used in the "selected state" i.e when a mailbox is selected
abstract class SelectedStateCommand {

    /*These limits are 20 octets less than the recommended limits, in order to compensate for the length of the command
    tag, the space after the tag and the CRLF at the end of the command (these are not taken into account when
    calculating the length of the command)
     */
    private static final int LENGTH_LIMIT_WITHOUT_CONDSTORE = 980;
    private static final int LENGTH_LIMIT_WITH_CONDSTORE = 8172;

    ImapConnection connection;
    ImapFolder folder;

    Set<Long> idSet;
    List<Range> idRanges;

    SelectedStateCommand(ImapConnection connection, ImapFolder folder) {
        this.connection = connection;
        this.folder = folder;
    }

    abstract String createCommandString();

    abstract Builder newBuilder();

    private List<SelectedStateCommand> splitCommand(int lengthLimit) {

        List<SelectedStateCommand> commands = new ArrayList<>();

        if (idSet != null || idRanges != null) {

            while ((idSet != null && !idSet.isEmpty()) || (idRanges != null && !idRanges.isEmpty())) {

                Builder builder = this.newBuilder()
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
                    } else {
                        break;
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

    void addIds(StringBuilder builder) {

        if (idSet != null || idRanges != null) {

            optimizeGroupings();

            if (idSet != null) {
                builder.append(ImapUtility.join(",", idSet));
            }
            if (idRanges != null) {
                if (idSet != null) {
                    builder.append(",");
                }
                builder.append(ImapUtility.join(",", idRanges));
            }
            builder.append(" ");
        }
    }

    public SelectedStateResponse execute() throws MessagingException {
        return null;
    }

    List<List<ImapResponse>> executeInternal() throws IOException, MessagingException {

        List<SelectedStateCommand> commands;
        String commandString = createCommandString();
        if (commandString.length() > getCommandLengthLimit()) {
            commands = Collections.unmodifiableList(splitCommand(getCommandLengthLimit()));
        } else {
            commands = Collections.singletonList(this);
        }

        List<List<ImapResponse>> responses = new ArrayList<>();
        for (SelectedStateCommand command : commands) {
            responses.add(folder.executeSimpleCommand(command.createCommandString()));
        }
        return responses;
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

        SelectedStateCommand newCommand = builder.build();
        this.idSet = newCommand.idSet;
        this.idRanges = newCommand.idRanges;
    }

    private void checkAndAddIds(Builder builder, List<Long> idList, int start, int end) {
        if (start == end) {
            builder.addId(idList.get(start));
        } else {
            builder.addIdRange(idList.get(start), idList.get(end));
        }
    }

    private int getCommandLengthLimit() throws IOException, MessagingException  {
        boolean condstoreSupported = connection.isCondstoreCapable();
        if (condstoreSupported) {
            return LENGTH_LIMIT_WITH_CONDSTORE;
        } else {
            return LENGTH_LIMIT_WITHOUT_CONDSTORE;
        }
    }

    static abstract class Builder<C extends SelectedStateCommand, B extends Builder<C, B>> {

        C command;
        B builder;

        abstract C createCommand(ImapConnection connection, ImapFolder folder);
        abstract B createBuilder();

        public Builder(ImapConnection connection, ImapFolder folder) {
            command = createCommand(connection, folder);
            builder = createBuilder();
        }

        public B idSet(Collection<Long> idSet) {
            if (idSet != null) {
                command.idSet = new TreeSet<>(idSet);
            } else {
                command.idSet = null;
            }
            return builder;
        }

        B addId(Long id) {
            if (command.idSet == null) {
                command.idSet = new TreeSet<>();
            }
            command.idSet.add(id);
            return builder;
        }

        B idRanges(List<Range> idRanges) {
            command.idRanges = idRanges;
            return builder;
        }

        public B addIdRange(Long start, Long end) {
            if (command.idRanges == null) {
                command.idRanges = new ArrayList<>();
            }
            command.idRanges.add(new Range(start, end));
            return builder;
        }

        public B allIds(boolean allIds) {
            if (allIds) {
                command.idSet = null;
                command.idRanges = Collections.singletonList(new Range(Range.FIRST_ID, Range.LAST_ID));
            }
            return builder;
        }

        public B onlyHighestId(boolean onlyHighestId) {
            if (onlyHighestId) {
                command.idSet = null;
                command.idRanges = Collections.singletonList(new Range(Range.LAST_ID, Range.LAST_ID));
            }
            return builder;
        }

        public C build() {
            return command;
        }

    }

    private static class Range {

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
