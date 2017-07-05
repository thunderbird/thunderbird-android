package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fsck.k9.mail.store.imap.selectedstate.command.FolderSelectedStateCommand.ContiguousIdGroup;
import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createNonContiguousIdSet;
import static junit.framework.Assert.assertEquals;


public class ImapCommandSplitterTest {

    @Test
    public void splitCommand_withManyNonContiguousIds_shouldSplitCommand() throws Exception {
        List<Long> ids = createNonContiguousIdSet(10000, 10500, 2);
        TestCommand command = createTestCommand(ids);

        List<FolderSelectedStateCommand> commands = ImapCommandSplitter.splitCommand(command, 980);

        assertEquals(commands.get(0).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10000, 10324, 2)));
        assertEquals(commands.get(1).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10326, 10500, 2)));
    }

    @Test
    public void splitCommand_withContiguousAndNonContiguousIds_shouldGroupIdsAndSplitCommand() throws Exception {
        List<Long> firstIdSet = createNonContiguousIdSet(10000, 10300, 2);
        List<Long> secondIdSet = createNonContiguousIdSet(10301, 10399, 1);
        List<Long> thirdIdSet = createNonContiguousIdSet(10400, 10500, 2);
        List<Long> idSet = new ArrayList<>(firstIdSet.size() + secondIdSet.size() + thirdIdSet.size());
        idSet.addAll(firstIdSet);
        idSet.addAll(secondIdSet);
        idSet.addAll(thirdIdSet);
        TestCommand command = createTestCommand(idSet);

        List<FolderSelectedStateCommand> commands = ImapCommandSplitter.splitCommand(command, 980);

        List<Long> firstCommandIds = createNonContiguousIdSet(10000, 10298, 2);
        firstCommandIds.addAll(createNonContiguousIdSet(10402, 10426, 2));
        assertEquals(commands.get(0).getIdSet(), new TreeSet<>(firstCommandIds));
        assertEquals(commands.get(1).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10428, 10500, 2)));
        assertEquals(commands.get(1).getIdGroups().get(0).getStart().longValue(), 10300L);
        assertEquals(commands.get(1).getIdGroups().get(0).getEnd().longValue(), 10400L);
    }

    @Test
    public void optimizeGroupings_withContiguousIds_shouldGroupIdsCorrectly() {
        TestCommand command = new TestCommand.Builder()
                .idSet(createNonContiguousIdSet(1, 100, 1))
                .build();

        ImapCommandSplitter.optimizeGroupings(command);

        verifyIdGroup(command, 1, 100);
    }

    @Test
    public void optimizeGroupings_withContiguousAndNonContiguousIds_shouldGroupIdsCorrectly() {
        List<Long> idSet = createNonContiguousIdSet(1, 100, 1);
        Set<Long> finalIdSet = new HashSet<>(createNonContiguousIdSet(300, 400, 2));
        idSet.addAll(finalIdSet);
        TestCommand command = new TestCommand.Builder()
                .idSet(idSet)
                .addIdGroup(101L, 115L)
                .build();

        ImapCommandSplitter.optimizeGroupings(command);

        assertEquals(command.getIdSet(), finalIdSet);
        verifyIdGroup(command, 1, 115);
    }

    private TestCommand createTestCommand(List<Long> ids) {
        TestCommand.Builder builder = new TestCommand.Builder();
        if (ids != null) {
            builder.idSet(ids);
        }
        return builder.build();
    }

    private void verifyIdGroup(TestCommand command, long start, long end) {
        List<ContiguousIdGroup> idGroups = command.getIdGroups();
        assertEquals(idGroups.size(), 1);

        ContiguousIdGroup idGroup = idGroups.get(0);
        assertEquals(idGroup.getStart(), Long.valueOf(start));
        assertEquals(idGroup.getEnd(), Long.valueOf(end));
    }
}
