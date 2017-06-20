package com.fsck.k9.mail.store.imap.command;


import java.util.Collections;
import java.util.Date;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;


public class UidSearchCommandTest {

    private static final String TEST_QUERY_STRING = "query";
    private static final String TEST_MESSAGE_ID = "<00000000.0000000@example.org>";
    private static final Date TEST_SINCE = new Date(10000000000L);
    private static final Set<Flag> TEST_FORBIDDEN_FLAGS = Collections.singleton(Flag.DELETED);

    private static final String TEST_SEARCH_KEY_QUERY_STRING = "TEXT \"query\"";
    private static final String TEST_SEARCH_KEY_MESSAGE_ID = "HEADER MESSAGE-ID \"<00000000.0000000@example.org>\"";
    private static final String TEST_SEARCH_KEY_SINCE = "SINCE 26-Apr-1970";;
    private static final String TEST_SEARCH_KEY_FORBIDDEN_FLAGS = "NOT DELETED";

    private ImapCommandFactory commandFactory;
    private ImapConnection connection;
    private ArgumentCaptor<String> commandStringCaptor;

    @Before
    public void setUp() {
        connection = Mockito.mock(ImapConnection.class);
        commandFactory = ImapCommandFactory.create(connection, null);
        commandStringCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    public void execute_shouldIssueProperCommand() throws Exception {
        UidSearchCommand command = buildCommand();

        command.execute();

        verify(connection).executeSimpleCommand(commandStringCaptor.capture(), Mockito.eq(false));
        String commandString = commandStringCaptor.getValue();
        assertTrue(commandString.contains(TEST_SEARCH_KEY_QUERY_STRING));
        assertTrue(commandString.contains(TEST_SEARCH_KEY_MESSAGE_ID));
        assertTrue(commandString.contains(TEST_SEARCH_KEY_SINCE));
        assertTrue(commandString.contains(TEST_SEARCH_KEY_FORBIDDEN_FLAGS));
    }

    private UidSearchCommand buildCommand() {
        return commandFactory.createUidSearchCommandBuilder(Mockito.mock(ImapFolder.class), null)
                .performFullTextSearch(true)
                .queryString(TEST_QUERY_STRING)
                .messageId(TEST_MESSAGE_ID)
                .since(TEST_SINCE)
                .forbiddenFlags(TEST_FORBIDDEN_FLAGS)
                .build();
    }
}
