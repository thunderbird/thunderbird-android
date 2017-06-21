package com.fsck.k9.mail.store.imap.command;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.store.imap.ImapConnection;
import com.fsck.k9.mail.store.imap.ImapFolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.verify;


public class UidStoreCommandTest {

    private static final List<Long> TEST_UID_SET = Arrays.asList(1L, 2L, 3L);
    private static final boolean TEST_VALUE = true;
    private static final Set<Flag> TEST_FLAG_SET = Collections.singleton(Flag.DELETED);

    private static final String TEST_UID_SET_OUTPUT = "1:3";
    private static final String TEST_VALUE_OUTPUT = "+FLAGS";
    private static final String TEST_FLAG_SET_OUTPUT = "(\\Deleted)";

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
        UidStoreCommand command = buildCommand();

        command.execute();

        verify(connection).executeSimpleCommand(commandStringCaptor.capture(), Mockito.eq(false));
        String commandString = commandStringCaptor.getValue();
        assertTrue(commandString.contains(TEST_UID_SET_OUTPUT));
        assertTrue(commandString.contains(TEST_VALUE_OUTPUT));
        assertTrue(commandString.contains(TEST_FLAG_SET_OUTPUT));
    }

    private UidStoreCommand buildCommand() {
        return commandFactory.createUidStoreCommandBuilder(Mockito.mock(ImapFolder.class))
                .idSet(TEST_UID_SET)
                .value(TEST_VALUE)
                .flagSet(TEST_FLAG_SET)
                .build();
    }
}
