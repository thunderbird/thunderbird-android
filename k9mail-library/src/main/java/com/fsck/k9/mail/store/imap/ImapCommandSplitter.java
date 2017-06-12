package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fsck.k9.mail.store.imap.FolderSelectedStateCommand.ContiguousIdGroup;


class ImapCommandSplitter {

    static List<String> splitCommand(FolderSelectedStateCommand command, int lengthLimit) {
        if (command.getIdSet().isEmpty() && command.getIdGroups().isEmpty()) {
            throw new IllegalStateException("The constructed command is too long but does not contain ids");
        }

        List<String> commands = new ArrayList<>();
        Set<Long> idSet = new TreeSet<>(command.getIdSet());
        List<ContiguousIdGroup> idGroups = new ArrayList<>(command.getIdGroups());

        while (!idSet.isEmpty() || !idGroups.isEmpty()) {
            command.clearIds();

            int length = command.createCommandString().length();
            while (length < lengthLimit) {
                if (!idSet.isEmpty()) {
                    Long first = idSet.iterator().next();
                    length += (String.valueOf(first).length() + 1);
                    if (length < lengthLimit) {
                        command.addId(first);
                        idSet.remove(first);
                    } else {
                        break;
                    }
                } else if (!idGroups.isEmpty()) {
                    ContiguousIdGroup first = idGroups.iterator().next();
                    length += (first.toString().length() + 1);
                    if (length < lengthLimit) {
                        command.addIdGroup(first.getStart(), first.getEnd());
                        idGroups.remove(first);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            commands.add(command.createCommandString());
        }
        return commands;
    }

    static void optimizeGroupings(FolderSelectedStateCommand command) {
        Set<Long> idSet = command.getIdSet();
        List<ContiguousIdGroup> idGroups = command.getIdGroups();

        if (idSet.isEmpty() && idGroups.isEmpty()) {
            return;
        }
        if (idGroups.size() == 1 && idGroups.get(0).getEnd() == ContiguousIdGroup.LAST_ID) {
            return;
        }

        TreeSet<Long> fullIdSet = new TreeSet<>();
        fullIdSet.addAll(idSet);
        for (ContiguousIdGroup idGroup : idGroups) {
            for (long i = idGroup.getStart();i <= idGroup.getEnd();i++) {
                fullIdSet.add(i);
            }
        }

        command.clearIds();
        List<Long> idList = new ArrayList<>(fullIdSet);
        int start = 0;

        for (int i = 1; i < idList.size();i++) {
            if (idList.get(i - 1) + 1 != idList.get(i)) {
                checkAndAddIds(command, idList, start, i - 1);
                start = i;
            }
        }
        checkAndAddIds(command, idList, start, idList.size() - 1);
    }

    private static void checkAndAddIds(FolderSelectedStateCommand command, List<Long> idList,
                                       int start, int end) {
        if (start == end) {
            command.addId(idList.get(start));
        } else {
            command.addIdGroup(idList.get(start), idList.get(end));
        }
    }
}
