package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fsck.k9.mail.store.imap.IdGrouper.GroupedIds;
import com.google.common.collect.Sets;
import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createNonContiguousIdSet;
import static java.util.Collections.singletonList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class ImapCommandSplitterTest {
    private static final String COMMAND_PREFIX = "UID COPY";
    private static final String COMMAND_SUFFIX = "\"Destination\"";


    @Test
    public void splitCommand_withManyNonContiguousIds_shouldSplitCommand() throws Exception {
        Set<Long> ids = createNonContiguousIdSet(10000, 10500, 2);
        GroupedIds groupedIds = new GroupedIds(ids, Collections.<IdGrouper.ContiguousIdGroup>emptyList());

        List<String> commands = ImapCommandSplitter.splitCommand(COMMAND_PREFIX, COMMAND_SUFFIX, groupedIds, 980);

        assertEquals(commands.size(), 2);
        assertCommandLengthLimit(commands, 980);
        verifyCommandString(commands.get(0), createNonContiguousIdSet(10000, 10316, 2));
        verifyCommandString(commands.get(1), createNonContiguousIdSet(10318, 10500, 2));
    }

    @Test
    public void splitCommand_withContiguousAndNonContiguousIds_shouldGroupIdsAndSplitCommand() throws Exception {
        Set<Long> idSet = Sets.union(
                createNonContiguousIdSet(10000, 10298, 2),
                createNonContiguousIdSet(10402, 10500, 2));
        List<IdGrouper.ContiguousIdGroup> idGroups = singletonList(new IdGrouper.ContiguousIdGroup(10300L, 10400L));
        GroupedIds groupedIds = new GroupedIds(idSet, idGroups);

        List<String> commands = ImapCommandSplitter.splitCommand(COMMAND_PREFIX, COMMAND_SUFFIX, groupedIds, 980);

        assertEquals(commands.size(), 2);
        assertCommandLengthLimit(commands, 980);
        verifyCommandString(commands.get(0), Sets.union(
                createNonContiguousIdSet(10000, 10298, 2),
                createNonContiguousIdSet(10402, 10418, 2)));
        verifyCommandString(commands.get(1), createNonContiguousIdSet(10420, 10500, 2), "10300:10400");
    }

    @Test
    public void splitCommand_withEmptySuffix_shouldCreateCommandWithoutTrailingSpace() throws Exception {
        Set<Long> ids = createNonContiguousIdSet(1, 2, 1);
        GroupedIds groupedIds = new GroupedIds(ids, Collections.<IdGrouper.ContiguousIdGroup>emptyList());

        List<String> commands = ImapCommandSplitter.splitCommand("UID SEARCH UID", "", groupedIds, 980);

        assertEquals(commands.size(), 1);
        assertEquals("UID SEARCH UID 1,2", commands.get(0));
    }


    private void assertCommandLengthLimit(List<String> commands, int lengthLimit) {
        for (String command : commands) {
            assertFalse("Command is too long (" + command.length() + " > " + lengthLimit + ")",
                    command.length() > lengthLimit);
        }
    }

    private void verifyCommandString(String actualCommand, Set<Long> ids) {
        verifyCommandString(actualCommand, ids, null);
    }

    private void verifyCommandString(String actualCommand, Set<Long> ids, String idGroupString) {
        Set<Long> sortedIds = new TreeSet<>(ids);
        StringBuilder expectedCommandBuilder = new StringBuilder(COMMAND_PREFIX)
                .append(" ")
                .append(ImapUtility.join(",", sortedIds));

        if (idGroupString != null) {
            expectedCommandBuilder.append(',').append(idGroupString);
        }

        expectedCommandBuilder.append(" ").append(COMMAND_SUFFIX);
        String expectedCommand = expectedCommandBuilder.toString();

        assertEquals(expectedCommand, actualCommand);
    }
}
