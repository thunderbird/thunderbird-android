package com.fsck.k9.mail.store.imap.selectedstate.command;


import java.util.ArrayList;
import java.util.Arrays;
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

        command.executeInternal();

        verify(folder).executeSimpleCommand(ImapUtility.join(",", createNonContiguousIdSet(10000, 10324, 2)));
        verify(folder).executeSimpleCommand(ImapUtility.join(",", createNonContiguousIdSet(10326, 10500, 2)));
    }

    @Test
    public void executeInternal_withContiguousIds_shouldGroupIds() throws Exception {
        List<Long> ids = createNonContiguousIdSet(10000, 10500, 1);
        TestCommand command = createTestCommand(ids);

        command.executeInternal();

        verify(folder).executeSimpleCommand("10000:10500");
    }

    @Test
    public void executeInternal_withContiguousAndNonContiguousIds_shouldGroupIds() throws Exception {
        List<Long> ids = Arrays.asList(1L, 3L, 5L, 6L, 7L, 8L, 12L, 15L);
        TestCommand command = createTestCommand(ids);

        command.executeInternal();

        verify(folder).executeSimpleCommand("1,3,12,15,5:8");
    }

    @Test
    public void executeInternal_withAllIds_shouldCreateProperCommand() throws Exception {
        TestCommand command = new TestCommand.Builder(connection, folder)
                .allIds(true)
                .build();

        command.executeInternal();

        verify(folder).executeSimpleCommand("1:*");
    }

    @Test
    public void executeInternal_withOnlyHighestId_shouldCreateProperCommand() throws Exception {
        TestCommand command = new TestCommand.Builder(connection, folder)
                .onlyHighestId(true)
                .build();

        command.executeInternal();

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
        TestCommand.Builder builder = new TestCommand.Builder(connection, folder);
        if (ids != null) {
            builder.idSet(ids);
        }
        return builder.build();
    }

    private static class TestCommand extends SelectedStateCommand {

        private TestCommand(ImapConnection connection, ImapFolder folder) {
            super(connection, folder);
        }

        @Override
        String createCommandString() {
            StringBuilder builder = new StringBuilder();
            super.addIds(builder);
            return builder.toString().trim();
        }

        @Override
        Builder newBuilder() {
            return new Builder(connection, folder);
        }

        static class Builder extends SelectedStateCommand.Builder<TestCommand, Builder> {

            public Builder(ImapConnection connection, ImapFolder folder) {
                super(connection, folder);
            }

            @Override
            TestCommand createCommand(ImapConnection connection, ImapFolder folder) {
                return new TestCommand(connection, folder);
            }

            @Override
            Builder createBuilder() {
                return this;
            }
        }
    }
}
