package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.internal.util.collections.Sets.newSet;


public class FolderSelectedStateCommandTest {
    @Test
    public void createCombinedIdString_withIdSet_shouldCreateRespectiveString() {
        TestCommand command = TestCommand.createWithIdSet(newSet(1L, 3L, 5L));

        String idString = command.createCombinedIdString();

        assertEquals(idString, "1,3,5 ");
    }

    @Test
    public void createCombinedIdString_withIdGroup_shouldCreateRespectiveString() {
        TestCommand command = TestCommand.createWithIdGroup(1L, 10L);

        String idString = command.createCombinedIdString();

        assertEquals(idString, "1:10 ");
    }

    @Test
    public void createCombinedIdString_withAllIds_shouldCreateRespectiveString() {
        TestCommand command = TestCommand.createWithIdSet(Collections.<Long>emptySet());
        command.useAllIds(true);

        String idString = command.createCombinedIdString();

        assertEquals(idString, "1:* ");
    }

    @Test
    public void createCombinedIdString_withOnlyHighestId_shouldCreateRespectiveString() {
        TestCommand command = TestCommand.createWithIdSet(Collections.<Long>emptySet());
        command.useOnlyHighestId(true);

        String idString = command.createCombinedIdString();

        assertEquals(idString, "*:* ");
    }

    @Test
    public void createCombinedIdString_withIdSetAndGroup_shouldCreateRespectiveString() {
        TestCommand command = TestCommand.createWithIdSetAndGroup(newSet(1L, 2L, 3L),
                5L, 10L);

        String idString = command.createCombinedIdString();

        assertEquals(idString, "1,2,3,5:10 ");
    }

    @Test
    public void optimizeAndSplit_withShortCommand_shouldNotSplitCommand() throws Exception {
        TestCommand command = TestCommand.createWithIdSet(newSet(1L, 2L, 3L));

        List<String> splitCommands = command.optimizeAndSplit(false);

        assertEquals(splitCommands.size(), 1);
    }

    @Test
    public void optimizeAndSplit_withLongCommand_shouldSplitCommand() throws Exception {
        TestCommand command = TestCommand.createWithIdSet(ImapResponseHelper
                .createNonContiguousIdSet(10000L, 10500L, 2));

        List<String> splitCommands = command.optimizeAndSplit(false);

        assertEquals(splitCommands.size(), 2);
    }
}
