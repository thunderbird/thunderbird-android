package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;


public class ImapCommandSplitterTest {

    @Test
    public void executeInternal_withManyNonContiguousIds_shouldSplitCommand() throws Exception {
        List<Long> ids = createNonContiguousIdSet(10000, 10500, 2);
        TestCommand command = createTestCommand(ids);

        List<SelectedStateCommand> commands = ImapCommandSplitter.splitCommand(command, 980);

        assertEquals(commands.get(0).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10000, 10324, 2)));
        assertEquals(commands.get(1).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10326, 10500, 2)));
    }

    @Test
    public void executeInternal_withContiguousAndNonContiguousIds_shouldGroupIdsAndSplitCommand() throws Exception {
        List<Long> firstIdSet = createNonContiguousIdSet(10000, 10300, 2);
        List<Long> secondIdSet = createNonContiguousIdSet(10301, 10399, 1);
        List<Long> thirdIdSet = createNonContiguousIdSet(10400, 10500, 2);
        List<Long> idSet = new ArrayList<>(firstIdSet.size() + secondIdSet.size() + thirdIdSet.size());
        idSet.addAll(firstIdSet);
        idSet.addAll(secondIdSet);
        idSet.addAll(thirdIdSet);
        TestCommand command = createTestCommand(idSet);

        List<SelectedStateCommand> commands = ImapCommandSplitter.splitCommand(command, 980);

        List<Long> firstCommandIds = createNonContiguousIdSet(10000, 10298, 2);
        firstCommandIds.addAll(createNonContiguousIdSet(10402, 10426, 2));
        assertEquals(commands.get(0).getIdSet(), new TreeSet<>(firstCommandIds));
        assertEquals(commands.get(1).getIdSet(), new TreeSet<>(createNonContiguousIdSet(10428, 10500, 2)));
        assertEquals(commands.get(1).getIdGroups().get(0).getStart().longValue(), 10300L);
        assertEquals(commands.get(1).getIdGroups().get(0).getEnd().longValue(), 10400L);
    }

    private List<Long> createNonContiguousIdSet(long start, long end, int interval) {
        List<Long> ids = new ArrayList<>();
        for (long i = start;i <= end;i += interval) {
            ids.add(i);
        }
        return ids;
    }

    private TestCommand createTestCommand(List<Long> ids) {
        TestCommand.Builder builder = new TestCommand.Builder();
        if (ids != null) {
            builder.idSet(ids);
        }
        return builder.build();
    }

    private static class TestCommand extends SelectedStateCommand {

        private TestCommand() {
        }

        @Override
        String createCommandString() {
            return createCombinedIdString().trim();
        }

        @Override
        Builder newBuilder() {
            return new Builder();
        }

        static class Builder extends SelectedStateCommand.Builder<TestCommand, Builder> {

            @Override
            TestCommand createCommand() {
                return new TestCommand();
            }

            @Override
            Builder createBuilder() {
                return this;
            }
        }
    }
}
