package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fsck.k9.mail.store.imap.selectedstate.command.FolderSelectedStateCommand.Builder;
import com.fsck.k9.mail.store.imap.selectedstate.command.FolderSelectedStateCommand.ContiguousIdGroup;


class ImapCommandSplitter {

    static List<FolderSelectedStateCommand> splitCommand(FolderSelectedStateCommand command, int lengthLimit) {
        List<FolderSelectedStateCommand> commands = new ArrayList<>();

        if (command.getIdSet() != null || command.getIdGroups() != null) {
            command = optimizeGroupings(command);
            Set<Long> idSet = command.getIdSet();
            List<ContiguousIdGroup> idGroups = command.getIdGroups();

            while ((idSet != null && !idSet.isEmpty()) || (idGroups != null && !idGroups.isEmpty())) {
                Builder builder = command.newBuilder()
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

                    } else if (idGroups != null && !idGroups.isEmpty()) {
                        ContiguousIdGroup first = command.idGroups.iterator().next();
                        length += (first.toString().length() + 1);
                        if (length < lengthLimit) {
                            builder.addIdGroup(first.getStart(), first.getEnd());
                            idGroups.remove(first);
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
            throw new IllegalStateException("The constructed command is too long but does not contain ids");
        }
        return commands;
    }

    private static FolderSelectedStateCommand optimizeGroupings(FolderSelectedStateCommand command) {
        Set<Long> idSet = command.getIdSet();
        List<ContiguousIdGroup> idGroups = command.getIdGroups();
        if (idGroups != null && idGroups.get(0).getEnd() == ContiguousIdGroup.LAST_ID) {
            return command;
        }

        TreeSet<Long> fullIdSet = new TreeSet<>();
        if (idSet != null) {
            fullIdSet.addAll(command.idSet);
        }
        if (idGroups != null) {
            for (ContiguousIdGroup idGroup : idGroups) {
                for (long i = idGroup.getStart();i <= idGroup.getEnd();i++) {
                    fullIdSet.add(i);
                }
            }
        }

        Builder builder = command.newBuilder()
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
        FolderSelectedStateCommand tempCommand = builder.build();
        command.setIdSet(tempCommand.getIdSet());
        command.setIdGroups(tempCommand.getIdGroups());
        return command;
    }

    private static void checkAndAddIds(Builder builder, List<Long> idList, int start, int end) {
        if (start == end) {
            builder.addId(idList.get(start));
        } else {
            builder.addIdGroup(idList.get(start), idList.get(end));
        }
    }
}
