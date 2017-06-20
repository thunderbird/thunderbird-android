package com.fsck.k9.mail.store.imap.command;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.text.TextUtils;

import com.fsck.k9.mail.store.imap.ImapFolder;


//This is the base class for a command that takes sequence numbers/uids as an argument
abstract class SelectByIdCommand extends BaseCommand {

    ImapFolder folder;

    boolean useUids;
    Set<Long> idSet;
    List<Range> idRanges;

    SelectByIdCommand(ImapCommandFactory commandFactory) {
        super(commandFactory);
    }

    @Override
    String createCommandString() {
        return null;
    }

    abstract Builder newBuilder();

    @Override
    public List<SelectByIdCommand> splitCommand(int lengthLimit) {

        List<SelectByIdCommand> commands = new ArrayList<>();

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
            if (useUids) {
                builder.append("UID ");
            }

            optimizeGroupings();

            if (idSet != null) {
                builder.append(TextUtils.join(",", idSet));
            }
            if (idRanges != null) {
                if (idSet != null) {
                    builder.append(",");
                }
                builder.append(TextUtils.join(",", idRanges));
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

        SelectByIdCommand newCommand = builder.build();
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

    static abstract class Builder<C extends SelectByIdCommand, B extends Builder<C, B>> {

        C command;
        B builder;

        abstract C createCommand();
        abstract B createBuilder();

        public Builder(ImapCommandFactory commandFactory, ImapFolder folder) {
            command = createCommand();
            builder = createBuilder();
            command.commandFactory = commandFactory;
            command.folder = folder;
        }

        public B useUids(boolean useUids) {
            command.useUids = useUids;
            return builder;
        }

        public B idSet(Collection<Long> idSet) {
            if (idSet != null) {
                command.idSet = new HashSet<>(idSet);
            } else {
                command.idSet = null;
            }
            return builder;
        }

        public B addId(Long id) {
            if (command.idSet == null) {
                command.idSet = new HashSet<>();
            }
            command.idSet.add(id);
            return builder;
        }

        public B idRanges(List<Range> idRanges) {
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
