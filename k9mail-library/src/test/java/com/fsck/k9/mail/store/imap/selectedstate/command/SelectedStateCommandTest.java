package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapUtility;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SelectedStateCommandTest {

    private ImapConnection connection;
    private ImapFolder folder;

    @Before
    public void setUp() throws Exception {
        connection = mock(ImapConnection.class);
        when(connection.isCondstoreCapable()).thenReturn(false);
        folder = mock(ImapFolder.class);
    }

    @Test
    public void executeInternal_withManyNonContiguousIds_shouldSplitCommand() throws Exception {
        List<Long> ids = createNonContiguousIdSet(10000, 10500, 2);
        TestCommand command = createTestCommand(ids);

        command.executeInternal(connection, folder);

        verify(folder).executeSimpleCommand(ImapUtility.join(",", createNonContiguousIdSet(10000, 10324, 2)));
        verify(folder).executeSimpleCommand(ImapUtility.join(",", createNonContiguousIdSet(10326, 10500, 2)));
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

        command.executeInternal(connection, folder);

        String firstCommand = ImapUtility.join(",", createNonContiguousIdSet(10000, 10298, 2)) + "," +
                ImapUtility.join(",", createNonContiguousIdSet(10402, 10426, 2));
        String secondCommand = ImapUtility.join(",", createNonContiguousIdSet(10428, 10500, 2)) + ",10300:10400";
        verify(folder).executeSimpleCommand(firstCommand);
        verify(folder).executeSimpleCommand(secondCommand);
    }

    @Test
    public void executeInternal_withAllIds_shouldCreateProperCommand() throws Exception {
        TestCommand command = new TestCommand.Builder()
                .allIds(true)
                .build();

        command.executeInternal(connection, folder);

        verify(folder).executeSimpleCommand("1:*");
    }

    @Test
    public void executeInternal_withOnlyHighestId_shouldCreateProperCommand() throws Exception {
        TestCommand command = new TestCommand.Builder()
                .onlyHighestId(true)
                .build();

        command.executeInternal(connection, folder);

        verify(folder).executeSimpleCommand("*:*");
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
