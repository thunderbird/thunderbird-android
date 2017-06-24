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

    Set<Long> idSet;
    List<ContiguousIdGroup> idGroups;

    abstract String createCommandString();

    abstract Builder newBuilder();

    String createCombinedIdString() {
        if (idSet != null || idGroups != null) {

            StringBuilder builder = new StringBuilder();

            if (idSet != null) {
                builder.append(ImapUtility.join(",", idSet));
            }
            if (idGroups != null) {
                if (idSet != null) {
                    builder.append(",");
                }
                builder.append(ImapUtility.join(",", idGroups));
            }
            builder.append(" ");
            return builder.toString();
        }
        return "";
    }

    public SelectedStateResponse execute(ImapConnection connection, ImapFolder folder) throws MessagingException {
        return null;
    }

    List<List<ImapResponse>> executeInternal(ImapConnection connection, ImapFolder folder)
            throws IOException, MessagingException {
        List<SelectedStateCommand> commands;
        String commandString = createCommandString();
        if (commandString.length() > getCommandLengthLimit(connection)) {
            commands = ImapCommandSplitter.splitCommand(this, getCommandLengthLimit(connection));
        } else {
            commands = Collections.singletonList(this);
        }

        List<List<ImapResponse>> responses = new ArrayList<>();
        for (SelectedStateCommand command : commands) {
            responses.add(folder.executeSimpleCommand(command.createCommandString()));
        }
        return responses;
    }

    Set<Long> getIdSet() {
        return idSet;
    }

    void setIdSet(Set<Long> idSet) {
        this.idSet = idSet;
    }

    List<ContiguousIdGroup> getIdGroups() {
        return idGroups;
    }

    void setIdGroups(List<ContiguousIdGroup> idGroups) {
        this.idGroups = idGroups;
    }

    private int getCommandLengthLimit(ImapConnection connection) throws IOException, MessagingException  {
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

        abstract C createCommand();
        abstract B createBuilder();

        public Builder() {
            command = createCommand();
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

        B idRanges(List<ContiguousIdGroup> idGroups) {
            command.idGroups = idGroups;
            return builder;
        }

        public B addIdGroup(Long start, Long end) {
            if (command.idGroups == null) {
                command.idGroups = new ArrayList<>();
            }
            command.idGroups.add(new ContiguousIdGroup(start, end));
            return builder;
        }

        public B allIds(boolean allIds) {
            if (allIds) {
                command.idSet = null;
                command.idGroups = Collections.singletonList(new ContiguousIdGroup(ContiguousIdGroup.FIRST_ID,
                        ContiguousIdGroup.LAST_ID));
            }
            return builder;
        }

        public B onlyHighestId(boolean onlyHighestId) {
            if (onlyHighestId) {
                command.idSet = null;
                command.idGroups = Collections.singletonList(new ContiguousIdGroup(ContiguousIdGroup.LAST_ID,
                        ContiguousIdGroup.LAST_ID));
            }
            return builder;
        }

        public C build() {
            return command;
        }
    }

    static class ContiguousIdGroup {
        static final long FIRST_ID = 1L;
        static final long LAST_ID = Long.MAX_VALUE;

        private Long start;
        private Long end;

        ContiguousIdGroup(Long start, Long end) {
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
