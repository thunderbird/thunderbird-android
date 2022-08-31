package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


class ImapCommandSplitter {
    static List<String> splitCommand(String prefix, String suffix, GroupedIds groupedIds, int lengthLimit) {
        List<String> commands = new ArrayList<>();
        Set<Long> workingIdSet = new TreeSet<>(groupedIds.ids);
        List<ContiguousIdGroup> workingIdGroups = new ArrayList<>(groupedIds.idGroups);

        int suffixLength = suffix.length();
        int staticCommandLength = prefix.length() + suffixLength + 2;
        while (!workingIdSet.isEmpty() || !workingIdGroups.isEmpty()) {
            StringBuilder commandBuilder = new StringBuilder(prefix).append(' ');
            int length = staticCommandLength;
            while (length < lengthLimit) {
                if (!workingIdSet.isEmpty()) {
                    Long id = workingIdSet.iterator().next();
                    String idString = Long.toString(id);

                    length += idString.length() + 1;
                    if (length >= lengthLimit) {
                        break;
                    }

                    commandBuilder.append(idString).append(',');
                    workingIdSet.remove(id);
                } else if (!workingIdGroups.isEmpty()) {
                    ContiguousIdGroup idGroup = workingIdGroups.iterator().next();
                    String idGroupString = idGroup.toString();

                    length += idGroupString.length() + 1;
                    if (length >= lengthLimit) {
                        break;
                    }

                    commandBuilder.append(idGroupString).append(',');
                    workingIdGroups.remove(idGroup);
                } else {
                    break;
                }
            }

            if (suffixLength != 0) {
                // Replace the last comma with a space
                commandBuilder.setCharAt(commandBuilder.length() - 1, ' ');
                commandBuilder.append(suffix);
            } else {
                // Remove last comma
                commandBuilder.setLength(commandBuilder.length() - 1);
            }

            String command = commandBuilder.toString();
            commands.add(command);
        }

        return commands;
    }
}
