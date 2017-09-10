package com.fsck.k9.mail.store.imap.selectedstate.command;


import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapResponseHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;


public class FolderSelectedStateCommandTest {

    private ImapConnection imapConnection;
    private ImapFolder imapFolder;

    @Before
    public void setUp() throws Exception {
        imapConnection = mock(ImapConnection.class);
        when(imapConnection.isCondstoreCapable()).thenReturn(false);
        imapFolder = mock(ImapFolder.class);
    }

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
    public void executeInternal_withShortCommand_shouldNotSplitCommand() throws Exception {
        TestCommand command = TestCommand.createWithIdSet(newSet(1L, 2L, 3L));

        command.executeInternal(imapConnection, imapFolder);

        verify(imapFolder, times(1)).executeSimpleCommand(anyString());
    }

    @Test
    public void executeInternal_withLongCommand_shouldSplitCommand() throws Exception {
        TestCommand command = TestCommand.createWithIdSet(ImapResponseHelper
                .createNonContiguousIdSet(10000L, 10500L, 2));

        command.executeInternal(imapConnection, imapFolder);

        verify(imapFolder, atLeast(2)).executeSimpleCommand(anyString());
    }
}
